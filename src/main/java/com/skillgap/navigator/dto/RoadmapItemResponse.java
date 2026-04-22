package com.skillgap.navigator.dto;

import com.skillgap.navigator.entity.ProgressStatus;
import com.skillgap.navigator.entity.SkillLevel;
import java.util.List;

public record RoadmapItemResponse(
        String skillName,
        SkillLevel currentLevel,
        SkillLevel targetLevel,
        ProgressStatus status,
        int completionPercent,
        int estimatedHours,
        int proficiencyScore,
        int weekNumber,
        String weekLabel,
        String weeklyGoal,
        String reminderMessage,
        String reminderDate,
        Integer confidenceScore,
        String notes,
        boolean bookmarked,
        String category,
        String description,
        List<String> prerequisites,
        PracticeProjectResponse miniProject,
        PracticeProjectResponse portfolioProject,
        List<GuidedLearningStepResponse> guidedPath,
        List<LearningResourceResponse> resources,
        List<DailyStudyTaskResponse> dailyPlan) {
}
