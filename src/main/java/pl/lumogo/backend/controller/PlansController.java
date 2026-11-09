package pl.lumogo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.lumogo.backend.dto.SavedPlanDto;
import pl.lumogo.backend.service.PlansService;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Plans", description = "Zapisane plany zalogowanego użytkownika")
public class PlansController {

    private final PlansService plansService;

    public PlansController(PlansService plansService) {
        this.plansService = plansService;
    }

    @GetMapping
    @Operation(summary = "Lista planów zalogowanego użytkownika")
    public List<SavedPlanDto> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return plansService.list(jwt.getSubject());
    }

    @PostMapping
    @Operation(summary = "Zapisz plan")
    @ApiResponse(responseCode = "201", description = "Plan zapisany")
    @ApiResponse(responseCode = "400", description = "Błędne dane (code: VALIDATION_ERROR)")
    public ResponseEntity<SavedPlanDto> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                               @RequestBody SavedPlanDto.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plansService.create(jwt.getSubject(), request));
    }

    @DeleteMapping("/{planId}")
    @Operation(summary = "Usuń własny plan")
    @ApiResponse(responseCode = "204", description = "Usunięto")
    @ApiResponse(responseCode = "404", description = "Brak takiego planu u tego użytkownika")
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long planId) {
        return plansService.delete(jwt.getSubject(), planId)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}
