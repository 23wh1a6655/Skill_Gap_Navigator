package com.skillgap.navigator.dto;

import java.util.List;

public record RoleAssessmentPayloadResponse(
        String roleName,
        int totalSkills,
        int totalQuestions,
        List<RoleAssessmentQuestionResponse> questions) {
}
