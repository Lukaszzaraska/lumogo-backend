package pl.lumogo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.lumogo.backend.dto.PreferencesDto;
import pl.lumogo.backend.service.PreferencesService;

@RestController
@RequestMapping("/api/preferences")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Preferences", description = "Preferencje planera zalogowanego użytkownika")
public class PreferencesController {

    private final PreferencesService preferencesService;

    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @GetMapping
    @Operation(summary = "Pobierz preferencje (domyślnie 30 min i brak ulubionych)")
    public PreferencesDto get(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return preferencesService.get(jwt.getSubject());
    }

    @PutMapping
    @Operation(summary = "Zapisz preferencje")
    public PreferencesDto update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @RequestBody PreferencesDto request) {
        return preferencesService.update(jwt.getSubject(), request);
    }
}
