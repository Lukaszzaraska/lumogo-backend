package pl.lumogo.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class WeatherConditionServiceTest {

    @Test
    void readsCurrentValuesNotMidnight() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
            .andRespond(withSuccess("""
                {"current":{"time":"2026-11-09T14:15","temperature_2m":12.34,
                 "relative_humidity_2m":55,"wind_speed_10m":7.2}}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("air-quality-api.open-meteo.com")))
            .andRespond(withSuccess("""
                {"current":{"time":"2026-11-09T14:00","us_aqi":31}}
                """, MediaType.APPLICATION_JSON));

        var result = new WeatherConditionService(rest).getTodayConditions(50.06, 19.94);

        assertThat(result.date).isEqualTo("2026-11-09");
        assertThat(result.temperatureC).isEqualTo(12.3);
        assertThat(result.humidity).isEqualTo(55.0);
        assertThat(result.windSpeedKmh).isEqualTo(7.2);
        assertThat(result.aqi).isEqualTo(31);
        assertThat(result.score).isBetween(0, 100);
        server.verify();
    }
}
