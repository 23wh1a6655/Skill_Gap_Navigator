package com.skillgap.navigator.dto;

import java.util.List;

public record ProgressAnalyticsResponse(
        int averageQuizScore,
        int latestQuizScore,
        int improvementDelta,
        int reassessmentCount,
        List<String> weakestSkills) {
}
