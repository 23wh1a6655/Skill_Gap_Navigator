package com.skillgap.navigator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/api/health")
    public Map<String, String> home() {
        return Map.of("status", "ok", "service", "Navigator API");
    }

}
