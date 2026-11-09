package pl.lumogo.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;

@Service
public class WeatherConditionService {

    private static final double DEFAULT_LATITUDE = 52.2297;
    private static final double DEFAULT_LONGITUDE = 21.0122;
    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String AIR_QUALITY_URL = "https://air-quality-api.open-meteo.com/v1/air-quality";

    /** Najwcześniejsza i najpóźniejsza godzina, w której okno może się zaczynać / kończyć. */
    static final int FIRST_START_HOUR = 6;
    static final int LAST_END_HOUR = 22;
    static final int MAX_DAYS_AHEAD = 4;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock;

    @Autowired
    public WeatherConditionService(RestTemplate restTemplate) {
        this(restTemplate, Clock.systemUTC());
    }

    WeatherConditionService(RestTemplate restTemplate, Clock clock) {
        this.restTemplate = restTemplate;
        this.clock = clock;
    }

    /**
     * Najlepsze okno na aktywność w podanym dniu (czas lokalny miejsca): ocena godzinowa
     * jak dla bieżących warunków, obniżona proporcjonalnie do szansy opadów.
     * Okno trwa durationMinutes, zaczyna się o pełnej godzinie między 6:00 a końcem dnia (22:00).
     */
    public BestWindowResponse findBestWindow(Double latitude, Double longitude, LocalDate date, int durationMinutes) {
        if (durationMinutes < 15 || durationMinutes > 240) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "durationMinutes musi być w zakresie 15-240");
        }
        LocalDate todayUtc = LocalDate.now(clock);
        if (date.isBefore(todayUtc.minusDays(1)) || date.isAfter(todayUtc.plusDays(MAX_DAYS_AHEAD + 1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data musi być w zakresie od dziś do " + MAX_DAYS_AHEAD + " dni naprzód");
        }
        double lat = latitude != null ? latitude : DEFAULT_LATITUDE;
        double lon = longitude != null ? longitude : DEFAULT_LONGITUDE;

        JsonNode weather = fetchJson(WEATHER_URL + "?latitude=" + lat + "&longitude=" + lon
            + "&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,precipitation_probability"
            + "&wind_speed_unit=kmh&timezone=auto&start_date=" + date + "&end_date=" + date);

        LocalDateTime localNow = LocalDateTime.now(clock).plusSeconds(weather.path("utc_offset_seconds").asLong(0));
        LocalDate today = localNow.toLocalDate();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data musi być w zakresie od dziś do " + MAX_DAYS_AHEAD + " dni naprzód");
        }

        JsonNode air = fetchJsonOrNull(AIR_QUALITY_URL + "?latitude=" + lat + "&longitude=" + lon
            + "&hourly=us_aqi&timezone=auto&start_date=" + date + "&end_date=" + date);

        JsonNode hourly = weather.path("hourly");
        JsonNode times = hourly.path("time");
        if (!times.isArray() || times.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Brak prognozy godzinowej");
        }

        int hoursNeeded = (int) Math.ceil(durationMinutes / 60.0);
        int[] scores = new int[24];
        boolean[] usable = new boolean[24];
        for (int i = 0; i < times.size(); i++) {
            LocalDateTime time = LocalDateTime.parse(times.get(i).asText());
            int hour = time.getHour();
            JsonNode temperature = hourly.path("temperature_2m").path(i);
            if (temperature.isNull() || temperature.isMissingNode()) {
                continue;
            }
            Integer aqi = air == null ? null : nullableInt(air.path("hourly").path("us_aqi").path(i));
            int base = calculateTotalScore(
                calculateTemperatureScore(temperature.asDouble(), date.getMonth()),
                calculateHumidityScore(hourly.path("relative_humidity_2m").path(i).asDouble(50)),
                calculateWindScore(hourly.path("wind_speed_10m").path(i).asDouble(0)),
                aqi == null ? 80 : calculateAqiScore(aqi));
            double rainProbability = hourly.path("precipitation_probability").path(i).asDouble(0);
            scores[hour] = (int) Math.round(base * (1 - 0.7 * rainProbability / 100.0));
            usable[hour] = !time.isBefore(localNow.withMinute(0).withSecond(0).withNano(0));
        }

        int bestStart = -1;
        double bestScore = -1;
        for (int start = FIRST_START_HOUR; start + hoursNeeded <= LAST_END_HOUR; start++) {
            double sum = 0;
            boolean ok = true;
            for (int h = start; h < start + hoursNeeded; h++) {
                if (!usable[h]) {
                    ok = false;
                    break;
                }
                sum += scores[h];
            }
            if (ok && sum / hoursNeeded > bestScore) {
                bestScore = sum / hoursNeeded;
                bestStart = start;
            }
        }
        if (bestStart < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Na ten dzień nie zostało już okno o tej długości");
        }

        int i = indexOfHour(times, bestStart);
        LocalTime start = LocalTime.of(bestStart, 0);
        int score = (int) Math.round(bestScore);
        return new BestWindowResponse(
            date.toString(),
            start.toString(),
            start.plusMinutes(durationMinutes).toString(),
            durationMinutes,
            score,
            scoreCategory(score),
            round(hourly.path("temperature_2m").path(i).asDouble(), 1),
            round(hourly.path("wind_speed_10m").path(i).asDouble(), 1),
            hourly.path("precipitation_probability").path(i).asInt(0),
            air == null ? null : nullableInt(air.path("hourly").path("us_aqi").path(i)),
            lat,
            lon
        );
    }

    private static int indexOfHour(JsonNode times, int hour) {
        for (int i = 0; i < times.size(); i++) {
            if (LocalDateTime.parse(times.get(i).asText()).getHour() == hour) {
                return i;
            }
        }
        return 0;
    }

    private static Integer nullableInt(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() ? null : (int) Math.round(node.asDouble());
    }

    /** Jakość powietrza jest dodatkiem: jej brak nie blokuje planowania. */
    private JsonNode fetchJsonOrNull(String url) {
        try {
            return fetchJson(url);
        } catch (ResponseStatusException ex) {
            return null;
        }
    }

    public WeatherConditionResponse getTodayConditions(Double latitude, Double longitude) {
        double lat = latitude != null ? latitude : DEFAULT_LATITUDE;
        double lon = longitude != null ? longitude : DEFAULT_LONGITUDE;

        JsonNode weatherNode = fetchJson(buildWeatherUrl(lat, lon));
        JsonNode airNode = fetchJson(buildAirQualityUrl(lat, lon));

        JsonNode current = weatherNode.path("current");
        if (current.isMissingNode() || current.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to retrieve current weather data");
        }

        double temperature = current.path("temperature_2m").asDouble(Double.NaN);
        double windSpeed = current.path("wind_speed_10m").asDouble(Double.NaN);
        double humidity = current.path("relative_humidity_2m").asDouble(0.0);
        String currentTime = current.path("time").asText();
        int aqi = (int) Math.round(airNode.path("current").path("us_aqi").asDouble(0.0));
        if (Double.isNaN(temperature) || Double.isNaN(windSpeed)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Incomplete current weather data");
        }

        LocalDate date = parseDateOnly(currentTime);

        int temperatureScore = calculateTemperatureScore(temperature, date.getMonth());
        int humidityScore = calculateHumidityScore(humidity);
        int windScore = calculateWindScore(windSpeed);
        int aqiScore = calculateAqiScore(aqi);

        int totalScore = calculateTotalScore(temperatureScore, humidityScore, windScore, aqiScore);

        return new WeatherConditionResponse(
            date.toString(),
            round(temperature, 1),
            round(windSpeed, 1),
            round(humidity, 1),
            aqi,
            totalScore,
            scoreCategory(totalScore),
            lat,
            lon
        );
    }

    private JsonNode fetchJson(String url) {
        try {
            String response = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(response);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to parse external weather API response", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "External weather API unavailable", ex);
        }
    }

    private String buildWeatherUrl(double latitude, double longitude) {
        return WEATHER_URL + "?latitude=" + latitude + "&longitude=" + longitude + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m&wind_speed_unit=kmh&timezone=auto";
    }

    private String buildAirQualityUrl(double latitude, double longitude) {
        return AIR_QUALITY_URL + "?latitude=" + latitude + "&longitude=" + longitude + "&current=us_aqi&timezone=auto";
    }

    private LocalDate parseDateOnly(String time) {
        try {
            return LocalDate.parse(time.substring(0, 10));
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }

    private int calculateTemperatureScore(double temperature, Month month) {
        double low;
        double high;
        switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> {
                low = -5; high = 5;
            }
            case MARCH, APRIL, MAY -> {
                low = 8; high = 18;
            }
            case JUNE, JULY, AUGUST -> {
                low = 16; high = 26;
            }
            default -> {
                low = 8; high = 18;
            }
        }

        if (temperature >= low && temperature <= high) {
            return 100;
        }

        double distance = temperature < low ? low - temperature : temperature - high;
        int score = (int) Math.max(0, 100 - distance * 5);
        return Math.min(score, 100);
    }

    private int calculateHumidityScore(double humidity) {
        double diff = Math.abs(humidity - 45.0);
        int score = (int) Math.max(0, 100 - diff * 1.5);
        return Math.min(score, 100);
    }

    private int calculateWindScore(double windSpeed) {
        if (windSpeed <= 10) {
            return 100;
        }
        if (windSpeed <= 20) {
            return 80;
        }
        if (windSpeed <= 30) {
            return 60;
        }
        return 40;
    }

    private int calculateAqiScore(int aqi) {
        if (aqi <= 50) {
            return 100;
        }
        if (aqi <= 100) {
            return 80;
        }
        if (aqi <= 150) {
            return 60;
        }
        if (aqi <= 200) {
            return 40;
        }
        if (aqi <= 300) {
            return 20;
        }
        return 0;
    }

    private int calculateTotalScore(int temperatureScore, int humidityScore, int windScore, int aqiScore) {
        double combined = temperatureScore * 0.3 + humidityScore * 0.2 + windScore * 0.1 + aqiScore * 0.4;
        return (int) Math.round(Math.max(0, Math.min(100, combined)));
    }

    private String scoreCategory(int score) {
        if (score >= 85) {
            return "Bardzo dobre";
        }
        if (score >= 70) {
            return "Dobre";
        }
        if (score >= 50) {
            return "Umiarkowane";
        }
        if (score >= 30) {
            return "Słabe";
        }
        return "Bardzo słabe";
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    public static class BestWindowResponse {
        public final String date;
        public final String start;
        public final String end;
        public final int durationMinutes;
        public final int score;
        public final String scoreCategory;
        public final double temperatureC;
        public final double windSpeedKmh;
        public final int precipitationProbability;
        public final Integer aqi;
        public final double latitude;
        public final double longitude;

        public BestWindowResponse(String date, String start, String end, int durationMinutes, int score, String scoreCategory,
                                  double temperatureC, double windSpeedKmh, int precipitationProbability, Integer aqi,
                                  double latitude, double longitude) {
            this.date = date;
            this.start = start;
            this.end = end;
            this.durationMinutes = durationMinutes;
            this.score = score;
            this.scoreCategory = scoreCategory;
            this.temperatureC = temperatureC;
            this.windSpeedKmh = windSpeedKmh;
            this.precipitationProbability = precipitationProbability;
            this.aqi = aqi;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static class WeatherConditionResponse {
        public final String date;
        public final double temperatureC;
        public final double windSpeedKmh;
        public final double humidity;
        public final int aqi;
        public final int score;
        public final String scoreCategory;
        public final double latitude;
        public final double longitude;

        public WeatherConditionResponse(String date, double temperatureC, double windSpeedKmh, double humidity, int aqi, int score, String scoreCategory, double latitude, double longitude) {
            this.date = date;
            this.temperatureC = temperatureC;
            this.windSpeedKmh = windSpeedKmh;
            this.humidity = humidity;
            this.aqi = aqi;
            this.score = score;
            this.scoreCategory = scoreCategory;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
