package com.skillgap.navigator.dto;

import java.util.List;

public record RoleAssessmentResultResponse(
        String roleName,
        int score,
        int totalQuestions,
        int percentage,
        List<String> feedback,
        List<SkillRequirementResponse> inferredSkills,
        SkillAssessmentResponse analysis) {
}
