package com.skillgap.navigator.dto;

import com.skillgap.navigator.entity.SkillLevel;

public record SkillRequirementResponse(
        String skillName,
        SkillLevel targetLevel,
        SkillLevel userLevel,
        String category,
        String description,
        boolean missing) {
}
