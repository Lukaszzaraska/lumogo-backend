package pl.lumogo.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.lumogo.backend.dto.AuthResponse;
import pl.lumogo.backend.dto.LoginRequest;
import pl.lumogo.backend.dto.RegisterRequest;
import pl.lumogo.backend.dto.UpdateProfileRequest;
import pl.lumogo.backend.model.AppUser;
import pl.lumogo.backend.repository.AppUserRepository;
import pl.lumogo.backend.security.TokenService;

import java.util.Locale;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       TokenService tokenService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegistration(request);

        String email = normalizeEmail(request.getEmail());
        if (appUserRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        AppUser appUser = new AppUser(
            request.getFirstName().trim(),
            request.getLastName() == null ? "" : request.getLastName().trim(),
            email,
            passwordEncoder.encode(request.getPassword())
        );

        appUserRepository.save(appUser);

        return new AuthResponse(
            "success",
            "Konto utworzone pomyślnie",
            appUser.getEmail(),
            appUser.getFirstName(),
            appUser.getLastName()
        ).withToken(tokenService.issue(appUser.getEmail()));
    }

    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() || request.getPassword() == null) {
            throw new IllegalArgumentException("E-mail i hasło są wymagane");
        }

        String email = normalizeEmail(request.getEmail());
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            email,
            request.getPassword()
        );

        // Rzuca AuthenticationException przy złym e-mailu lub haśle.
        authenticationManager.authenticate(authToken);

        AppUser appUser = appUserRepository.findByEmail(email)
            .orElseThrow(UserNotFoundException::new);

        return new AuthResponse(
            "success",
            "Zalogowano pomyślnie",
            appUser.getEmail(),
            appUser.getFirstName(),
            appUser.getLastName(),
            appUser.getInterests()
        ).withToken(tokenService.issue(appUser.getEmail()));
    }

    public AuthResponse getProfile(String email) {

        AppUser appUser = appUserRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(UserNotFoundException::new);

        return new AuthResponse(
            "success",
            "Profil pobrany pomyślnie",
            appUser.getEmail(),
            appUser.getFirstName(),
            appUser.getLastName(),
            appUser.getInterests()
        );
    }

    @Transactional
    public AuthResponse updateProfile(String email, UpdateProfileRequest request) {
        AppUser appUser = appUserRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(UserNotFoundException::new);

        boolean updated = false;

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            appUser.setFirstName(request.getFirstName().trim());
            updated = true;
        }

        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            appUser.setLastName(request.getLastName().trim());
            updated = true;
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new IllegalArgumentException("Aktualne hasło jest wymagane do zmiany hasła");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), appUser.getPassword())) {
                throw new IllegalArgumentException("Aktualne hasło jest nieprawidłowe");
            }
            if (request.getPasswordConfirm() == null || !request.getPassword().equals(request.getPasswordConfirm())) {
                throw new IllegalArgumentException("Hasła muszą być takie same");
            }
            validatePassword(request.getPassword());
            appUser.setPassword(passwordEncoder.encode(request.getPassword()));
            updated = true;
        }

        if (!updated) {
            return AuthResponse.error("NO_CHANGES", "Brak zmian do zapisania");
        }

        appUserRepository.save(appUser);

        return new AuthResponse(
            "success",
            "Profil zaktualizowany pomyślnie",
            appUser.getEmail(),
            appUser.getFirstName(),
            appUser.getLastName(),
            appUser.getInterests()
        );
    }

    /**
     * Ta sama reguła co w aplikacji mobilnej (RegistrationScreen):
     * min. 8 znaków, co najmniej jedna wielka litera i jeden znak specjalny.
     */
    static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Hasło jest wymagane");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Hasło musi mieć co najmniej 8 znaków");
        }
        if (!password.codePoints().anyMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("Hasło musi zawierać co najmniej jedną wielką literę");
        }
        if (!password.matches("(?s).*[^\\p{L}\\p{N}\\s].*")) {
            throw new IllegalArgumentException("Hasło musi zawierać co najmniej jeden znak specjalny");
        }
    }

    private void validateRegistration(RegisterRequest request) {
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Imię jest wymagane");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("E-mail jest wymagany");
        }
        if (request.getPasswordConfirm() == null || request.getPasswordConfirm().isBlank()) {
            throw new IllegalArgumentException("Potwierdzenie hasła jest wymagane");
        }
        if (request.getPassword() != null && !request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Hasła muszą być takie same");
        }
        validatePassword(request.getPassword());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
