package pl.lumogo.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BestWindowTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    /** 24 godziny: 12°C wszędzie, deszcz zawsze 90% poza 14:00 i 15:00. */
    private static String forecast(int utcOffsetSeconds) {
        String times = IntStream.range(0, 24).mapToObj(h -> "\"%sT%02d:00\"".formatted(DAY, h)).collect(Collectors.joining(","));
        String temps = IntStream.range(0, 24).mapToObj(h -> "12").collect(Collectors.joining(","));
        String hum = IntStream.range(0, 24).mapToObj(h -> "50").collect(Collectors.joining(","));
        String wind = IntStream.range(0, 24).mapToObj(h -> "5").collect(Collectors.joining(","));
        String rain = IntStream.range(0, 24).mapToObj(h -> h == 14 || h == 15 ? "0" : "90").collect(Collectors.joining(","));
        return """
            {"utc_offset_seconds":%d,"hourly":{"time":[%s],"temperature_2m":[%s],"relative_humidity_2m":[%s],
             "wind_speed_10m":[%s],"precipitation_probability":[%s]}}
            """.formatted(utcOffsetSeconds, times, temps, hum, wind, rain);
    }

    private static WeatherConditionService service(RestTemplate rest, String nowUtc) {
        return new WeatherConditionService(rest, Clock.fixed(Instant.parse(nowUtc), ZoneOffset.UTC));
    }

    @Test
    void picksDryHoursAndFormatsWindow() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(containsString("start_date=2026-11-10"))).andRespond(withSuccess(forecast(3600), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("air-quality"))).andRespond(withServerError());

        var window = service(rest, "2026-11-09T10:00:00Z").findBestWindow(50.0, 19.9, DAY, 90);

        assertThat(window.start).isEqualTo("14:00");
        assertThat(window.end).isEqualTo("15:30");
        assertThat(window.precipitationProbability).isZero();
        assertThat(window.aqi).isNull();
        server.verify();
    }

    @Test
    void todaySkipsHoursThatAlreadyPassed() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(containsString("forecast"))).andRespond(withSuccess(forecast(3600), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("air-quality"))).andRespond(withServerError());

        // 15:20 UTC = 16:20 czasu lokalnego: 14:00 i 15:00 już minęły
        var window = service(rest, "2026-11-10T15:20:00Z").findBestWindow(50.0, 19.9, DAY, 30);

        assertThat(window.start).isEqualTo("16:00");
    }

    @Test
    void rejectsDateTooFarAheadWithoutCallingApi() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer.bindTo(rest).build();
        assertThatThrownBy(() -> service(rest, "2026-11-09T10:00:00Z").findBestWindow(null, null, DAY.plusDays(10), 30))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }
}
