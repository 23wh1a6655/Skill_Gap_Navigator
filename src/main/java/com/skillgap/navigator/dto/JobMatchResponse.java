package com.skillgap.navigator.dto;

import java.util.List;

public record JobMatchResponse(
        String title,
        String company,
        String location,
        String url,
        int matchScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        String source) {
}
