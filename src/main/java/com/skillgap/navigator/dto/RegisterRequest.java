package com.skillgap.navigator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, message = "Full name must be at least 3 characters")
    private String username;
    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email is required")
    private String email;
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }
}
