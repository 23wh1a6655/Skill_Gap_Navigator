package com.skillgap.navigator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResumeAnalysisRequest(
        @NotNull(message = "User id is required")
        Long userId,
        @NotBlank(message = "Role is required")
        String role,
        @NotBlank(message = "Resume text is required")
        String resumeText) {
}
