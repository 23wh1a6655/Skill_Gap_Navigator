package com.skillgap.navigator.dto;

public record QuizAttemptSummary(
        String skillName,
        int percentage,
        boolean passed,
        String completedAt,
        int attemptNumber) {
}
