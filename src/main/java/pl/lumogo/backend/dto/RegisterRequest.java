package pl.lumogo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RegisterRequest", description = "Żądanie rejestracji nowego użytkownika")
public class RegisterRequest {
    @Schema(description = "Imię użytkownika", example = "Anna")
    private String firstName;
    @Schema(description = "Nazwisko użytkownika (opcjonalne)", example = "Kowalska", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String lastName;
    @Schema(description = "E-mail użytkownika", example = "anna@example.com")
    private String email;
    @Schema(description = "Hasło (min. 8 znaków, wielka litera, znak specjalny)", example = "Haslo!23")
    private String password;
    @Schema(description = "Potwierdzenie hasła (opcjonalne; jeśli podane, musi być takie samo)", example = "Haslo!23")
    private String passwordConfirm;

    @Schema(description = "Płeć z formularza (opcjonalna; \"-\" = nie podano)", example = "Woman")
    private String gender;

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}
