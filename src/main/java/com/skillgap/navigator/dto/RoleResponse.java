package com.skillgap.navigator.dto;

import java.util.List;

public record RoleResponse(
        String name,
        String description,
        String salarySignal,
        String hiringSignal,
        List<String> outcomes,
        List<SkillRequirementResponse> skills) {
}
