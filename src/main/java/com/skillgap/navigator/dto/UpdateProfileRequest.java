package com.skillgap.navigator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotNull(message = "User id is required")
        Long userId,
        @NotBlank(message = "Name is required")
        @Size(min = 3, message = "Name must be at least 3 characters")
        String fullName,
        String targetRole) {
}
