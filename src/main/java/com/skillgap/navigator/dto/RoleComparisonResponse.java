package com.skillgap.navigator.dto;

import java.util.List;

public record RoleComparisonResponse(
        String primaryRole,
        String compareRole,
        List<String> sharedSkills,
        List<String> primaryOnlySkills,
        List<String> compareOnlySkills) {
}
