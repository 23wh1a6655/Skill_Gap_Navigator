package com.skillgap.navigator.dto;

public record AchievementResponse(
        String title,
        String category,
        String description,
        String issuedAt) {
}
