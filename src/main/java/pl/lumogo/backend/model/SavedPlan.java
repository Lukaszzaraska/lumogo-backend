package pl.lumogo.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_plan", indexes = @Index(name = "idx_saved_plan_user", columnList = "user_id"))
public class SavedPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 16)
    private String activityEmoji;

    @Column(nullable = false, length = 80)
    private String activityLabel;

    @Column(nullable = false, length = 80)
    private String dateLabel;

    @Column(nullable = false, length = 5)
    private String startTime;

    @Column(nullable = false, length = 5)
    private String endTime;

    private LocalDate plannedDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected SavedPlan() {
    }

    public SavedPlan(AppUser user, String activityEmoji, String activityLabel, String dateLabel,
                     String startTime, String endTime, LocalDate plannedDate) {
        this.user = user;
        this.activityEmoji = activityEmoji;
        this.activityLabel = activityLabel;
        this.dateLabel = dateLabel;
        this.startTime = startTime;
        this.endTime = endTime;
        this.plannedDate = plannedDate;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getActivityEmoji() {
        return activityEmoji;
    }

    public String getActivityLabel() {
        return activityLabel;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
