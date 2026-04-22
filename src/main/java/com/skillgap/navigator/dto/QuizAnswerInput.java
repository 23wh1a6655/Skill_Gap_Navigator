package com.skillgap.navigator.dto;

import jakarta.validation.constraints.NotBlank;

public record QuizAnswerInput(
        @NotBlank(message = "Question id is required")
        String questionId,
        String selectedOption,
        String responseText) {
}
