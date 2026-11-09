package pl.lumogo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponse", description = "Odpowiedź na operację autentykacji")
public class AuthResponse {
    @Schema(description = "Status odpowiedzi", example = "success", allowableValues = {"success", "error"})
    private String status;
    @Schema(description = "Wiadomość", example = "Zalogowano pomyślnie")
    private String message;
    @Schema(description = "E-mail użytkownika", example = "anna@example.com")
    private String email;
    @Schema(description = "Imię użytkownika", example = "Anna")
    private String firstName;
    @Schema(description = "Nazwisko użytkownika", example = "Kowalska")
    private String lastName;
    @Schema(description = "Zainteresowania użytkownika jako tekst", example = "Sport, Muzyka, Podróże")
    private String interests;
    @Schema(description = "Identyfikator użytkownika", example = "1")
    private Long id;
    @Schema(description = "Płeć z formularza (null, jeśli nie podano)", example = "Woman")
    private String gender;
    @Schema(description = "Token JWT (Bearer) zwracany przy logowaniu i rejestracji")
    private String token;
    @Schema(description = "Kod błędu do tłumaczenia w aplikacji", example = "EMAIL_TAKEN",
        allowableValues = {"EMAIL_TAKEN", "INVALID_CREDENTIALS", "VALIDATION_ERROR", "USER_NOT_FOUND", "NO_CHANGES"})
    private String code;

    /** Odpowiedź z kompletem danych użytkownika. */
    public static AuthResponse of(pl.lumogo.backend.model.AppUser user, String message) {
        AuthResponse response = new AuthResponse("success", message, user.getEmail(), user.getFirstName(),
            user.getLastName(), user.getInterests());
        response.id = user.getId();
        response.gender = user.getGender();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getGender() {
        return gender;
    }

    public static AuthResponse error(String code, String message) {
        AuthResponse response = new AuthResponse("error", message);
        response.code = code;
        return response;
    }

    public AuthResponse withToken(String token) {
        this.token = token;
        return this;
    }

    public String getToken() {
        return token;
    }

    public String getCode() {
        return code;
    }

    public AuthResponse() {
    }

    public AuthResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public AuthResponse(String status, String message, String email, String firstName, String lastName) {
        this.status = status;
        this.message = message;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public AuthResponse(String status, String message, String email, String firstName, String lastName, String interests) {
        this.status = status;
        this.message = message;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.interests = interests;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }
}
