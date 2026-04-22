package com.skillgap.navigator.controller;

import com.skillgap.navigator.dto.AuthResponse;
import com.skillgap.navigator.dto.LoginRequest;
import com.skillgap.navigator.dto.RegisterRequest;
import com.skillgap.navigator.dto.UpdateProfileRequest;
import com.skillgap.navigator.dto.UserSummary;
import com.skillgap.navigator.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
    }

    @PutMapping("/profile")
    public UserSummary updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(request);
    }
}
