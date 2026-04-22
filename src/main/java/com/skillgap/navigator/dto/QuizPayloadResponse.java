package com.skillgap.navigator.dto;

import java.util.List;

public record QuizPayloadResponse(
        String skillName,
        String recommendedDifficulty,
        int confidenceScore,
        List<QuizQuestionResponse> questions) {
}
