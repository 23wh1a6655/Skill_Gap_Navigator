package com.skillgap.navigator.dto;

public record GuidedLearningStepResponse(
        int day,
        String title,
        String taskType,
        String goal,
        String expectedOutcome) {
}
