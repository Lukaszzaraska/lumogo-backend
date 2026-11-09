package pl.lumogo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Zmiana hasła zalogowanego użytkownika")
public class ChangePasswordRequest {

    @Schema(description = "Aktualne hasło", example = "Haslo!23")
    private String currentPassword;

    @Schema(description = "Nowe hasło (min. 8 znaków, wielka litera, znak specjalny)", example = "Nowe!haslo")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
