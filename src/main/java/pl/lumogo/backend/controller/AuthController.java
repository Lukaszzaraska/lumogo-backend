package pl.lumogo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.lumogo.backend.dto.AuthResponse;
import pl.lumogo.backend.dto.ChangePasswordRequest;
import pl.lumogo.backend.dto.LoginRequest;
import pl.lumogo.backend.dto.RegisterRequest;
import pl.lumogo.backend.dto.UpdateProfileRequest;
import pl.lumogo.backend.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Rejestracja, logowanie (token JWT) i profil zalogowanego użytkownika")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Rejestracja nowego użytkownika",
        description = "Nazwisko opcjonalne. Hasło: min. 8 znaków, 1 wielka litera, 1 znak specjalny. Zwraca token JWT.")
    @ApiResponse(responseCode = "200", description = "Użytkownik zarejestrowany, w odpowiedzi token")
    @ApiResponse(responseCode = "400", description = "Błąd walidacji (code: VALIDATION_ERROR)")
    @ApiResponse(responseCode = "409", description = "Konto z tym adresem e-mail już istnieje (code: EMAIL_TAKEN)")
    public ResponseEntity<AuthResponse> register(@RequestBody @Validated RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Logowanie użytkownika", description = "Zwraca dane użytkownika i token JWT")
    @ApiResponse(responseCode = "200", description = "Logowanie pomyślne, w odpowiedzi token")
    @ApiResponse(responseCode = "401", description = "Nieprawidłowy e-mail lub hasło (code: INVALID_CREDENTIALS)")
    public ResponseEntity<AuthResponse> login(@RequestBody @Validated LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Profil zalogowanego użytkownika", description = "Użytkownik z tokena (nagłówek Authorization: Bearer ...)")
    @ApiResponse(responseCode = "200", description = "Profil pobrany pomyślnie")
    @ApiResponse(responseCode = "401", description = "Brak lub nieważny token")
    @ApiResponse(responseCode = "404", description = "Konto nie istnieje (code: USER_NOT_FOUND)")
    public ResponseEntity<AuthResponse> profile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getProfile(jwt.getSubject()));
    }

    @PutMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Aktualizuj profil zalogowanego użytkownika", description = "Imię, nazwisko i hasło (zmiana hasła wymaga currentPassword)")
    @ApiResponse(responseCode = "200", description = "Profil zaktualizowany pomyślnie")
    @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane (code: VALIDATION_ERROR lub NO_CHANGES)")
    @ApiResponse(responseCode = "401", description = "Brak lub nieważny token")
    public ResponseEntity<AuthResponse> updateProfile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                      @RequestBody @Validated UpdateProfileRequest request) {
        AuthResponse response = authService.updateProfile(jwt.getSubject(), request);
        return "error".equals(response.getStatus())
            ? ResponseEntity.badRequest().body(response)
            : ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Zmień hasło zalogowanego użytkownika")
    @ApiResponse(responseCode = "204", description = "Hasło zmienione")
    @ApiResponse(responseCode = "400", description = "Złe aktualne hasło lub zbyt słabe nowe (code: VALIDATION_ERROR)")
    @ApiResponse(responseCode = "401", description = "Brak lub nieważny token")
    public ResponseEntity<Void> changePassword(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                               @RequestBody ChangePasswordRequest request) {
        authService.changePassword(jwt.getSubject(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/account")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Usuń konto zalogowanego użytkownika", description = "Usuwa konto, zapisane plany i preferencje. Wymagane przez Google Play.")
    @ApiResponse(responseCode = "204", description = "Konto usunięte")
    @ApiResponse(responseCode = "401", description = "Brak lub nieważny token")
    public ResponseEntity<Void> deleteAccount(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        authService.deleteAccount(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
