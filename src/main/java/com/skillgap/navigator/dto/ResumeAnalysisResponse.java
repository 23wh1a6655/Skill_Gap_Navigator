package com.skillgap.navigator.dto;

import java.util.List;

public record ResumeAnalysisResponse(
        String role,
        List<String> detectedSkills,
        List<String> missingSkills,
        String recommendation) {
}
