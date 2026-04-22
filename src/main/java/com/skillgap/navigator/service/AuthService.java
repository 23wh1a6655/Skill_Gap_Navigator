package com.skillgap.navigator.service;

import com.skillgap.navigator.config.JwtService;
import com.skillgap.navigator.dto.AuthResponse;
import com.skillgap.navigator.dto.LoginRequest;
import com.skillgap.navigator.dto.RegisterRequest;
import com.skillgap.navigator.dto.UpdateProfileRequest;
import com.skillgap.navigator.dto.UserSummary;
import com.skillgap.navigator.entity.User;
import com.skillgap.navigator.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public AuthResponse register(RegisterRequest request){
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResponseStatusException(BAD_REQUEST, "An account with this email already exists.");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose a different name for this account.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(null);
        user.setOnboardingComplete(false);

        User savedUser = userRepository.save(user);

        return new AuthResponse("Account created successfully.", jwtService.generateToken(savedUser.getEmail()), toSummary(savedUser));
    }

    public AuthResponse login(LoginRequest request){
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found."));

        if(passwordEncoder.matches(request.getPassword(), user.getPassword())){
            return new AuthResponse("Login successful.", jwtService.generateToken(user.getEmail()), toSummary(user));
        }

        throw new ResponseStatusException(UNAUTHORIZED, "Invalid password.");
    }

    public User getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !"anonymousUser".equals(authentication.getName())
                && !authentication.getName().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Access denied.");
        }
        return user;
    }

    public UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isOnboardingComplete());
    }

    public UserSummary updateProfile(UpdateProfileRequest request) {
        User user = getUser(request.userId());
        if (!user.getUsername().equalsIgnoreCase(request.fullName())
                && userRepository.existsByUsernameIgnoreCase(request.fullName())) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose a different name for this account.");
        }
        user.setUsername(request.fullName().trim());
        if (request.targetRole() != null && !request.targetRole().isBlank()) {
            user.setRole(request.targetRole().trim());
        }
        User saved = userRepository.save(user);
        return toSummary(saved);
    }
}
