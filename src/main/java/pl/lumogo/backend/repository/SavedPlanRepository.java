package pl.lumogo.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.lumogo.backend.model.AppUser;
import pl.lumogo.backend.model.SavedPlan;

import java.util.List;
import java.util.Optional;

public interface SavedPlanRepository extends JpaRepository<SavedPlan, Long> {

    List<SavedPlan> findByUserOrderByPlannedDateAscStartTimeAsc(AppUser user);

    Optional<SavedPlan> findByIdAndUser(Long id, AppUser user);

    long countByUser(AppUser user);

    void deleteByUser(AppUser user);
}
