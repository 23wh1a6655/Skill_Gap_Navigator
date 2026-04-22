package com.skillgap.navigator.dto;

import java.util.List;

public record PracticeProjectResponse(
        String title,
        String projectType,
        String difficulty,
        String estimatedDuration,
        String objective,
        List<String> deliverables) {
}
