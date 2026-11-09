package pl.lumogo.backend.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import pl.lumogo.backend.dto.AuthResponse;
import pl.lumogo.backend.dto.LoginRequest;
import pl.lumogo.backend.dto.RegisterRequest;
import pl.lumogo.backend.dto.UpdateProfileRequest;
import pl.lumogo.backend.service.AuthService;
import pl.lumogo.backend.service.EmailAlreadyUsedException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Rejestracja i logowanie użytkowników")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Rejestracja nowego użytkownika", description = "Nazwisko opcjonalne. Hasło: min. 8 znaków, 1 wielka litera, 1 znak specjalny")
    @ApiResponse(responseCode = "200", description = "Użytkownik zarejestrowany pomyślnie")
    @ApiResponse(responseCode = "400", description = "Błąd walidacji")
    @ApiResponse(responseCode = "409", description = "Konto z tym adresem e-mail już istnieje")
    public ResponseEntity<AuthResponse> register(@RequestBody @Validated RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (EmailAlreadyUsedException | DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthResponse("error", new EmailAlreadyUsedException().getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new AuthResponse("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Logowanie użytkownika", description = "Zwraca dane użytkownika po pomyślnym zalogowaniu")
    @ApiResponse(responseCode = "200", description = "Logowanie pomyślne")
    @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane logowania")
    public ResponseEntity<AuthResponse> login(@RequestBody @Validated LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new AuthResponse("error", "Nieprawidłowe dane logowania"));
        }
    }

    @GetMapping("/profile")
    @Operation(summary = "Pobierz profil użytkownika", description = "Zwraca dane profilu użytkownika po adresie e-mail")
    @ApiResponse(responseCode = "200", description = "Profil pobrany pomyślnie")
    @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane żądania")
    public ResponseEntity<AuthResponse> profile(@RequestParam String email) {
        try {
            return ResponseEntity.ok(authService.getProfile(email));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new AuthResponse("error", ex.getMessage()));
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "Aktualizuj profil użytkownika", description = "Umożliwia aktualizację imienia, nazwiska i hasła użytkownika")
    @ApiResponse(responseCode = "200", description = "Profil zaktualizowany pomyślnie")
    @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane żądania")
    public ResponseEntity<AuthResponse> updateProfile(@RequestBody @Validated UpdateProfileRequest request) {
        try {
            return ResponseEntity.ok(authService.updateProfile(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new AuthResponse("error", ex.getMessage()));
        }
    }
}
