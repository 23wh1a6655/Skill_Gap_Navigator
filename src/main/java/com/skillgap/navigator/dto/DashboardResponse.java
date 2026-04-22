package com.skillgap.navigator.dto;

import java.util.List;

public record DashboardResponse(
        String fullName,
        String targetRole,
        int readinessScore,
        int completionRate,
        int streakDays,
        int completedSkills,
        int inProgressSkills,
        int pendingSkills,
        int totalSkills,
        int totalEstimatedHours,
        List<RoadmapItemResponse> spotlight,
        List<QuizAttemptSummary> recentResults,
        List<AchievementResponse> achievements,
        ProgressAnalyticsResponse analytics,
        List<JobMatchResponse> jobMatches,
        List<WeakAreaInsightResponse> weakAreaInsights) {
}
