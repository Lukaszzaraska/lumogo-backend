package pl.lumogo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preferencje planera")
public record PreferencesDto(
    @Schema(description = "Domyślna długość aktywności w minutach", example = "30") int defaultDuration,
    @Schema(description = "Ulubione aktywności (id z aplikacji) po przecinku", example = "walking,running") String favoriteActivities
) {
}
