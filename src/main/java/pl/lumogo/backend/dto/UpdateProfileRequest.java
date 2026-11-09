package pl.lumogo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateProfileRequest", description = "Żądanie aktualizacji profilu użytkownika")
public class UpdateProfileRequest {
    @Schema(description = "E-mail użytkownika", example = "anna@example.com")
    private String email;
    @Schema(description = "Imię użytkownika", example = "Anna")
    private String firstName;
    @Schema(description = "Nazwisko użytkownika", example = "Kowalska")
    private String lastName;
    @Schema(description = "Nowe hasło użytkownika", example = "Haslo!23")
    private String password;
    @Schema(description = "Potwierdzenie nowego hasła", example = "Haslo!23")
    private String passwordConfirm;
    @Schema(description = "Aktualne hasło użytkownika", example = "Haslo!23")
    private String currentPassword;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
