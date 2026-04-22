package com.skillgap.navigator.controller;

import com.skillgap.navigator.dto.DashboardResponse;
import com.skillgap.navigator.dto.ResumeAnalysisRequest;
import com.skillgap.navigator.dto.ResumeAnalysisResponse;
import com.skillgap.navigator.service.NavigatorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
public class DashboardController {

    private final NavigatorService navigatorService;

    public DashboardController(NavigatorService navigatorService) {
        this.navigatorService = navigatorService;
    }

    @GetMapping("/{userId}")
    public DashboardResponse getDashboard(@PathVariable Long userId) {
        return navigatorService.getDashboard(userId);
    }

    @PostMapping("/resume-analysis")
    public ResumeAnalysisResponse analyzeResume(@Valid @RequestBody ResumeAnalysisRequest request) {
        return navigatorService.analyzeResume(request);
    }
}
