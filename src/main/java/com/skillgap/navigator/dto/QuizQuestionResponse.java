package com.skillgap.navigator.dto;

import java.util.List;

public record QuizQuestionResponse(
        String id,
        String prompt,
        List<String> options,
        String questionType,
        String difficulty,
        String concept,
        List<String> evaluationCriteria) {
}
