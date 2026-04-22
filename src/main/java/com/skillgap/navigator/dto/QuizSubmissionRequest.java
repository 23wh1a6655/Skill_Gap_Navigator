package com.skillgap.navigator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuizSubmissionRequest(
        @NotNull(message = "User id is required")
        Long userId,
        @NotBlank(message = "Skill is required")
        String skillName,
        Integer confidenceRating,
        @Valid
        List<QuizAnswerInput> answers) {
}
