package com.skillgap.navigator.dto;

import java.util.List;

public record RoleAssessmentQuestionResponse(
        String id,
        String skillName,
        String prompt,
        List<String> options,
        String questionType,
        String difficulty,
        String concept,
        List<String> evaluationCriteria) {
}
