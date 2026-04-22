package com.skillgap.navigator.dto;

import jakarta.validation.constraints.NotBlank;

public record InterviewAnswerInput(
        @NotBlank(message = "Question id is required")
        String questionId,
        @NotBlank(message = "Answer is required")
        String answer) {
}
