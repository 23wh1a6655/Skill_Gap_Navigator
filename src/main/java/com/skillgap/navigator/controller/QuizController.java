package com.skillgap.navigator.controller;

import com.skillgap.navigator.dto.QuizPayloadResponse;
import com.skillgap.navigator.dto.QuizResultResponse;
import com.skillgap.navigator.dto.QuizSubmissionRequest;
import com.skillgap.navigator.dto.MockInterviewPayloadResponse;
import com.skillgap.navigator.dto.MockInterviewResultResponse;
import com.skillgap.navigator.dto.MockInterviewSubmissionRequest;
import com.skillgap.navigator.dto.RoleAssessmentPayloadResponse;
import com.skillgap.navigator.dto.RoleAssessmentResultResponse;
import com.skillgap.navigator.dto.RoleAssessmentSubmissionRequest;
import com.skillgap.navigator.service.NavigatorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin
public class QuizController {

    private final NavigatorService navigatorService;

    public QuizController(NavigatorService navigatorService) {
        this.navigatorService = navigatorService;
    }

    @GetMapping
    public QuizPayloadResponse getQuiz(@RequestParam Long userId, @RequestParam String skill) {
        return navigatorService.getQuiz(userId, skill);
    }

    @GetMapping("/role-assessment")
    public RoleAssessmentPayloadResponse getRoleAssessment(@RequestParam Long userId, @RequestParam String role) {
        return navigatorService.getRoleAssessment(userId, role);
    }

    @PostMapping("/submit")
    public QuizResultResponse submitQuiz(@Valid @RequestBody QuizSubmissionRequest request) {
        return navigatorService.submitQuiz(request);
    }

    @PostMapping("/role-assessment/submit")
    public RoleAssessmentResultResponse submitRoleAssessment(@Valid @RequestBody RoleAssessmentSubmissionRequest request) {
        return navigatorService.submitRoleAssessment(request);
    }

    @GetMapping("/interview")
    public MockInterviewPayloadResponse getMockInterview(@RequestParam Long userId, @RequestParam String skill) {
        return navigatorService.getMockInterview(userId, skill);
    }

    @PostMapping("/interview/submit")
    public MockInterviewResultResponse submitMockInterview(@Valid @RequestBody MockInterviewSubmissionRequest request) {
        return navigatorService.evaluateMockInterview(request);
    }
}
