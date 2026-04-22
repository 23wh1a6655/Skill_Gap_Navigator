package com.skillgap.navigator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RoadmapPreferenceUpdateRequest(
        @NotNull(message = "User id is required")
        Long userId,
        @NotBlank(message = "Skill name is required")
        String skillName,
        String notes,
        Boolean bookmarked,
        Integer confidenceScore,
        LocalDate reminderDate) {
}
