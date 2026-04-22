package com.skillgap.navigator.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_progress")
public class LearningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String roleName;

    @Column(nullable = false)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel currentLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel targetLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status;

    private int completionPercent;

    private int estimatedHours;

    private int displayOrder;

    private Integer proficiencyScore;

    private Integer weeklyGoalHours;

    private Integer confidenceScore;

    @Lob
    private String notes;

    private Boolean bookmarked;

    private LocalDate reminderDate;

    private String reminderMessage;

    private String milestoneName;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public SkillLevel getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(SkillLevel currentLevel) {
        this.currentLevel = currentLevel;
    }

    public SkillLevel getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(SkillLevel targetLevel) {
        this.targetLevel = targetLevel;
    }

    public ProgressStatus getStatus() {
        return status;
    }

    public void setStatus(ProgressStatus status) {
        this.status = status;
    }

    public int getCompletionPercent() {
        return completionPercent;
    }

    public void setCompletionPercent(int completionPercent) {
        this.completionPercent = completionPercent;
    }

    public int getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(int estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Integer getProficiencyScore() {
        return proficiencyScore;
    }

    public void setProficiencyScore(Integer proficiencyScore) {
        this.proficiencyScore = proficiencyScore;
    }

    public Integer getWeeklyGoalHours() {
        return weeklyGoalHours;
    }

    public void setWeeklyGoalHours(Integer weeklyGoalHours) {
        this.weeklyGoalHours = weeklyGoalHours;
    }

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(Boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public LocalDate getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(LocalDate reminderDate) {
        this.reminderDate = reminderDate;
    }

    public String getReminderMessage() {
        return reminderMessage;
    }

    public void setReminderMessage(String reminderMessage) {
        this.reminderMessage = reminderMessage;
    }

    public String getMilestoneName() {
        return milestoneName;
    }

    public void setMilestoneName(String milestoneName) {
        this.milestoneName = milestoneName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
