package pl.lumogo.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import pl.lumogo.backend.dto.AuthResponse;
import pl.lumogo.backend.dto.InterestsRequest;
import pl.lumogo.backend.service.InterestsService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interests")
@Tag(name = "Interests", description = "Zarządzanie zainteresowaniami użytkownika")
public class InterestsController {

    private final InterestsService interestsService;

    public InterestsController(InterestsService interestsService) {
        this.interestsService = interestsService;
    }

    @GetMapping("/options")
    @Operation(summary = "Lista dostępnych zainteresowań", description = "Zwraca listę 10 kategorii sportowych do wyboru")
    @ApiResponse(responseCode = "200", description = "Lista kategorii zainteresowań")
    public ResponseEntity<Object> getOptions() {
        return ResponseEntity.ok(interestsService.getAvailableInterests());
    }

    @GetMapping("/user")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Zainteresowania zalogowanego użytkownika", description = "Użytkownik z tokena")
    @ApiResponse(responseCode = "200", description = "Zainteresowania pobrane")
    @ApiResponse(responseCode = "401", description = "Brak lub nieważny token")
    public ResponseEntity<Map<String, Object>> getUserInterests(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("email", email);
        response.put("interests", interestsService.getUserInterests(email).orElse(List.of()));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Zapisz zainteresowania zalogowanego użytkownika", description = "Lista nazw z /api/interests/options")
    @ApiResponse(responseCode = "200", description = "Zainteresowania zapisane pomyślnie")
    @ApiResponse(responseCode = "400", description = "Błąd w żądaniu")
    @ApiResponse(responseCode = "401", description = "Brak lub nieważny token")
    public ResponseEntity<AuthResponse> saveInterests(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                      @RequestBody InterestsRequest request) {
        if (request.getInterests() == null || request.getInterests().isEmpty()) {
            return ResponseEntity.badRequest().body(AuthResponse.error("VALIDATION_ERROR", "Wybierz przynajmniej jedno zainteresowanie"));
        }

        return interestsService.saveUserInterests(jwt.getSubject(), request.getInterests())
            .map(user -> ResponseEntity.ok(new AuthResponse("success", "Zainteresowania zapisane", user.getEmail(), user.getFirstName(), user.getLastName())))
            .orElseGet(() -> ResponseEntity.status(404).body(AuthResponse.error("USER_NOT_FOUND", "Użytkownik nie znaleziony")));
    }
}
