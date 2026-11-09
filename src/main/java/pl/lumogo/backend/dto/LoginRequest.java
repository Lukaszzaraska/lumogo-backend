package pl.lumogo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginRequest", description = "Żądanie logowania")
public class LoginRequest {
    @Schema(description = "E-mail użytkownika", example = "anna@example.com")
    private String email;
    @Schema(description = "Hasło użytkownika", example = "Haslo!23")
    private String password;

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
}
