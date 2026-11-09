package pl.lumogo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.lumogo.backend.model.SavedPlan;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Zapisany plan aktywności")
public record SavedPlanDto(
    Long id,
    Long userId,
    String activityEmoji,
    String activityLabel,
    String dateLabel,
    String startTime,
    String endTime,
    LocalDate plannedDate,
    LocalDateTime createdAt
) {
    public static SavedPlanDto from(SavedPlan plan) {
        return new SavedPlanDto(plan.getId(), plan.getUser().getId(), plan.getActivityEmoji(), plan.getActivityLabel(),
            plan.getDateLabel(), plan.getStartTime(), plan.getEndTime(), plan.getPlannedDate(), plan.getCreatedAt());
    }

    @Schema(description = "Nowy plan; użytkownik jest brany z tokena (userId w treści jest ignorowane)")
    public record Create(
        @Schema(example = "🚶") String activityEmoji,
        @Schema(example = "Walking") String activityLabel,
        @Schema(example = "Tomorrow, Tue 10 Nov") String dateLabel,
        @Schema(example = "14:00") String startTime,
        @Schema(example = "14:30") String endTime,
        @Schema(example = "2026-11-10") LocalDate plannedDate
    ) {
    }
}
