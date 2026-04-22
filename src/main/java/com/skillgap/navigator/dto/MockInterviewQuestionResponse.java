package com.skillgap.navigator.dto;

import java.util.List;

public record MockInterviewQuestionResponse(
        String id,
        String prompt,
        String focusArea,
        List<String> evaluationPoints) {
}
