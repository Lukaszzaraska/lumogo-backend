package pl.lumogo.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.lumogo.backend.dto.PreferencesDto;
import pl.lumogo.backend.model.AppUser;
import pl.lumogo.backend.repository.AppUserRepository;

@Service
public class PreferencesService {

    static final int DEFAULT_DURATION = 30;

    private final AppUserRepository users;

    public PreferencesService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PreferencesDto get(String email) {
        return toDto(user(email));
    }

    @Transactional
    public PreferencesDto update(String email, PreferencesDto request) {
        if (request.defaultDuration() < 15 || request.defaultDuration() > 240) {
            throw new IllegalArgumentException("Domyślna długość musi być w zakresie 15-240 minut");
        }
        String activities = request.favoriteActivities() == null ? "" : request.favoriteActivities().trim();
        if (activities.length() > 500) {
            throw new IllegalArgumentException("Za długa lista aktywności");
        }
        AppUser user = user(email);
        user.setDefaultDuration(request.defaultDuration());
        user.setFavoriteActivities(activities);
        return toDto(users.save(user));
    }

    private static PreferencesDto toDto(AppUser user) {
        return new PreferencesDto(
            user.getDefaultDuration() == null ? DEFAULT_DURATION : user.getDefaultDuration(),
            user.getFavoriteActivities() == null ? "" : user.getFavoriteActivities());
    }

    private AppUser user(String email) {
        return users.findByEmail(email).orElseThrow(UserNotFoundException::new);
    }
}
