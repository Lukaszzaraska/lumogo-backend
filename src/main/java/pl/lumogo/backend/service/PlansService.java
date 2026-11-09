package pl.lumogo.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.lumogo.backend.dto.SavedPlanDto;
import pl.lumogo.backend.model.AppUser;
import pl.lumogo.backend.model.SavedPlan;
import pl.lumogo.backend.repository.AppUserRepository;
import pl.lumogo.backend.repository.SavedPlanRepository;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class PlansService {

    /** Ochrona przed zaśmiecaniem bazy przez jedno konto. */
    static final int MAX_PLANS_PER_USER = 200;
    private static final Pattern TIME = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final SavedPlanRepository plans;
    private final AppUserRepository users;

    public PlansService(SavedPlanRepository plans, AppUserRepository users) {
        this.plans = plans;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<SavedPlanDto> list(String email) {
        return plans.findByUserOrderByPlannedDateAscStartTimeAsc(user(email)).stream().map(SavedPlanDto::from).toList();
    }

    @Transactional
    public SavedPlanDto create(String email, SavedPlanDto.Create request) {
        AppUser user = user(email);
        require(request.activityLabel(), "activityLabel", 80);
        require(request.activityEmoji(), "activityEmoji", 16);
        require(request.dateLabel(), "dateLabel", 80);
        if (request.startTime() == null || !TIME.matcher(request.startTime()).matches()
            || request.endTime() == null || !TIME.matcher(request.endTime()).matches()) {
            throw new IllegalArgumentException("Godziny muszą mieć format HH:mm");
        }
        if (plans.countByUser(user) >= MAX_PLANS_PER_USER) {
            throw new IllegalArgumentException("Osiągnięto limit zapisanych planów (" + MAX_PLANS_PER_USER + ")");
        }
        SavedPlan plan = new SavedPlan(user, request.activityEmoji().trim(), request.activityLabel().trim(),
            request.dateLabel().trim(), request.startTime(), request.endTime(), request.plannedDate());
        return SavedPlanDto.from(plans.save(plan));
    }

    /** Usuwa tylko własny plan; cudzy wygląda jak nieistniejący. */
    @Transactional
    public boolean delete(String email, Long planId) {
        return plans.findByIdAndUser(planId, user(email))
            .map(plan -> {
                plans.delete(plan);
                return true;
            })
            .orElse(false);
    }

    private AppUser user(String email) {
        return users.findByEmail(email).orElseThrow(UserNotFoundException::new);
    }

    private static void require(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + field + " jest wymagane");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException("Pole " + field + " może mieć najwyżej " + maxLength + " znaków");
        }
    }
}
