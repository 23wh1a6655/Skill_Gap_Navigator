package com.skillgap.navigator.dto;

import java.util.List;

public record SkillAssessmentResponse(
        String role,
        int readinessScore,
        int weeklyHours,
        int estimatedWeeks,
        List<SkillRequirementResponse> requiredSkills,
        List<SkillRequirementResponse> missingSkills,
        List<SkillRequirementResponse> strengths,
        List<String> milestones,
        String recommendation) {
}
