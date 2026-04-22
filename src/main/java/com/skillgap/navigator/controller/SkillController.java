package com.skillgap.navigator.controller;

import com.skillgap.navigator.dto.SkillAssessmentRequest;
import com.skillgap.navigator.dto.SkillAssessmentResponse;
import com.skillgap.navigator.service.NavigatorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skills")
@CrossOrigin
public class SkillController {

    private final NavigatorService navigatorService;

    public SkillController(NavigatorService navigatorService) {
        this.navigatorService = navigatorService;
    }

    @PostMapping("/analysis")
    public SkillAssessmentResponse analyzeSkills(@Valid @RequestBody SkillAssessmentRequest request) {
        return navigatorService.analyzeSkills(request);
    }
}
