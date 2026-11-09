package pl.lumogo.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import pl.lumogo.backend.service.WeatherConditionService;
import pl.lumogo.backend.service.WeatherConditionService.WeatherConditionResponse;
import pl.lumogo.backend.service.WeatherConditionService.BestWindowResponse;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@Tag(name = "Weather", description = "Warunki pogodowe i współczynnik przydatności do biegania")
public class WeatherConditionController {

    private final WeatherConditionService weatherConditionService;

    public WeatherConditionController(WeatherConditionService weatherConditionService) {
        this.weatherConditionService = weatherConditionService;
    }

    @GetMapping("/conditions/today")
    @Operation(summary = "Warunki pogodowe na dzisiaj", description = "Zwraca temperaturę, prędkość wiatru, wilgotność, AQI oraz ocenę przydatności do biegania (0-100). Domyślnie: Warszawa.")
    @ApiResponse(responseCode = "200", description = "Warunki pogodowe pobrane pomyślnie")
    public ResponseEntity<WeatherConditionResponse> getTodayConditions(
        @RequestParam(required = false) @Parameter(description = "Szerokość geograficzna (domyślnie 52.2297 - Warszawa)") Double lat,
        @RequestParam(required = false) @Parameter(description = "Długość geograficzna (domyślnie 21.0122 - Warszawa)") Double lon
    ) {
        return ResponseEntity.ok(weatherConditionService.getTodayConditions(lat, lon));
    }

    @GetMapping("/conditions/best-window")
    @Operation(summary = "Najlepsze okno na aktywność w danym dniu",
        description = "Na podstawie prognozy godzinowej (temperatura, wilgotność, wiatr, szansa opadów, AQI). "
            + "Okno zaczyna się o pełnej godzinie między 6:00 a 22:00 czasu lokalnego. Data: od dziś do 4 dni naprzód.")
    @ApiResponse(responseCode = "200", description = "Najlepsze okno")
    @ApiResponse(responseCode = "400", description = "Nieprawidłowa data lub długość")
    @ApiResponse(responseCode = "422", description = "Na dziś nie zostało już okno tej długości")
    public ResponseEntity<BestWindowResponse> getBestWindow(
        @RequestParam(required = false) @Parameter(description = "Szerokość geograficzna (domyślnie Warszawa)") Double lat,
        @RequestParam(required = false) @Parameter(description = "Długość geograficzna (domyślnie Warszawa)") Double lon,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Dzień, np. 2026-11-10") LocalDate date,
        @RequestParam(defaultValue = "30") @Parameter(description = "Długość aktywności w minutach (15-240)") int durationMinutes
    ) {
        return ResponseEntity.ok(weatherConditionService.findBestWindow(lat, lon, date, durationMinutes));
    }
}
