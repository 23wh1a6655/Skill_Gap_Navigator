package com.skillgap.navigator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoleAssessmentSubmissionRequest(
        @NotNull(message = "User id is required")
        Long userId,
        @NotBlank(message = "Role is required")
        String role,
        Integer weeklyHours,
        @Valid
        List<QuizAnswerInput> answers) {
}
