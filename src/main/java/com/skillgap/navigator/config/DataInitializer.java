package com.skillgap.navigator.config;

import com.skillgap.navigator.entity.User;
import com.skillgap.navigator.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedDemoUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmailIgnoreCase("demo@navigator.app")) {
                User user = new User();
                user.setUsername("Demo Learner");
                user.setEmail("demo@navigator.app");
                user.setPassword(passwordEncoder.encode("demo123"));
                user.setOnboardingComplete(false);
                userRepository.save(user);
            }
        };
    }
}
