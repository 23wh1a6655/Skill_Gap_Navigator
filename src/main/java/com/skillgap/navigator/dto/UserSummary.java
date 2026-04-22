package com.skillgap.navigator.dto;

public record UserSummary(
        Long id,
        String fullName,
        String email,
        String targetRole,
        boolean onboardingComplete) {
}
