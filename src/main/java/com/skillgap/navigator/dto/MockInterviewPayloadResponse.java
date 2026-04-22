package com.skillgap.navigator.dto;

import java.util.List;

public record MockInterviewPayloadResponse(
        String roleName,
        String skillName,
        List<MockInterviewQuestionResponse> questions) {
}
