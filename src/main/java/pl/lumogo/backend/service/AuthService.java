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
import pl.lumogo.backend.repository.SavedPlanRepository;
import pl.lumogo.backend.security.TokenService;

import java.util.Locale;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final SavedPlanRepository savedPlanRepository;

    public AuthService(AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       TokenService tokenService,
                       SavedPlanRepository savedPlanRepository) {
        this.savedPlanRepository = savedPlanRepository;
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
        appUser.setGender(normalizeGender(request.getGender()));

        appUserRepository.save(appUser);

        return AuthResponse.of(appUser, "Konto utworzone pomyślnie").withToken(tokenService.issue(appUser.getEmail()));
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

        return AuthResponse.of(appUser, "Zalogowano pomyślnie").withToken(tokenService.issue(appUser.getEmail()));
    }

    public AuthResponse getProfile(String email) {

        AppUser appUser = appUserRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(UserNotFoundException::new);

        return AuthResponse.of(appUser, "Profil pobrany pomyślnie");
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

        if (request.getGender() != null) {
            String gender = normalizeGender(request.getGender());
            if (!java.util.Objects.equals(gender, appUser.getGender())) {
                appUser.setGender(gender);
                updated = true;
            }
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

        return AuthResponse.of(appUser, "Profil zaktualizowany pomyślnie");
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
        // Potwierdzenie sprawdza aplikacja; jeśli klient je wyśle, musi się zgadzać.
        if (request.getPasswordConfirm() != null && request.getPassword() != null
            && !request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Hasła muszą być takie same");
        }
        validatePassword(request.getPassword());
    }

    /** Zmiana hasła zalogowanego użytkownika. */
    public void changePassword(String email, String currentPassword, String newPassword) {
        AppUser appUser = appUserRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(UserNotFoundException::new);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, appUser.getPassword())) {
            throw new IllegalArgumentException("Aktualne hasło jest nieprawidłowe");
        }
        if (newPassword != null && newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("Nowe hasło musi różnić się od aktualnego");
        }
        validatePassword(newPassword);
        appUser.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(appUser);
    }

    /** Usuwa konto razem z planami (prawo do usunięcia danych, wymóg Google Play). */
    @org.springframework.transaction.annotation.Transactional
    public void deleteAccount(String email) {
        appUserRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            savedPlanRepository.deleteByUser(user);
            appUserRepository.delete(user);
        });
    }

    private static String normalizeGender(String gender) {
        if (gender == null) {
            return null;
        }
        String trimmed = gender.trim();
        return trimmed.isEmpty() || "-".equals(trimmed) ? null : trimmed;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
