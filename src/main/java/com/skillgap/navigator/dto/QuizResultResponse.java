package com.skillgap.navigator.dto;

import java.util.List;

public record QuizResultResponse(
        String skillName,
        int score,
        int totalQuestions,
        int percentage,
        boolean passed,
        int submittedConfidence,
        int measuredConfidence,
        List<String> feedback,
        List<String> weakAreas,
        List<String> nextActions,
        List<WeakAreaInsightResponse> weakAreaInsights,
        int attemptNumber,
        int improvementDelta) {
}
