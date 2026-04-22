package com.skillgap.navigator.dto;

import java.util.List;

public record MockInterviewResultResponse(
        String roleName,
        String skillName,
        int score,
        List<String> strengths,
        List<String> improvements,
        List<String> nextSteps) {
}
