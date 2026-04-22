package com.skillgap.navigator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MockInterviewSubmissionRequest(
        @NotNull(message = "User id is required")
        Long userId,
        @NotBlank(message = "Role is required")
        String roleName,
        @NotBlank(message = "Skill is required")
        String skillName,
        @Valid
        List<InterviewAnswerInput> answers) {
}
