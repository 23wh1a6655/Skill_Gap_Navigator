package com.skillgap.navigator.dto;

import com.skillgap.navigator.entity.SkillLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SkillInput(
        @NotBlank(message = "Skill name is required")
        String skillName,
        @NotNull(message = "Skill level is required")
        SkillLevel level) {
}
