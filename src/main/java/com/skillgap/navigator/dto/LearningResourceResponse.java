package com.skillgap.navigator.dto;

public record LearningResourceResponse(
        String title,
        String platform,
        String type,
        String duration,
        String url,
        String description,
        String level,
        String language,
        String budget,
        double rating,
        int practicalScore,
        int rank,
        String recommendationReason) {
}
