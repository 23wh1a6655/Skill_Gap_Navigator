package com.skillgap.navigator.dto;

public record AuthResponse(String message, String token, UserSummary user) {
}
