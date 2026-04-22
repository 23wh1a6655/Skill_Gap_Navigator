package com.skillgap.navigator.dto;

import com.skillgap.navigator.entity.ProgressStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProgressUpdateRequest(
        @NotNull(message = "User id is required")
        Long userId,
        @NotBlank(message = "Skill is required")
        String skillName,
        @NotNull(message = "Status is required")
        ProgressStatus status,
        @Min(value = 0, message = "Completion cannot be negative")
        @Max(value = 100, message = "Completion cannot exceed 100")
        int completionPercent) {
}
