package com.skillgap.navigator.controller;

import com.skillgap.navigator.dto.ProgressUpdateRequest;
import com.skillgap.navigator.dto.RoadmapPreferenceUpdateRequest;
import com.skillgap.navigator.dto.RoadmapItemResponse;
import com.skillgap.navigator.service.NavigatorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roadmap")
@CrossOrigin
public class RoadmapController {

    private final NavigatorService navigatorService;

    public RoadmapController(NavigatorService navigatorService) {
        this.navigatorService = navigatorService;
    }

    @GetMapping("/{userId}")
    public List<RoadmapItemResponse> getRoadmap(@PathVariable Long userId) {
        return navigatorService.getRoadmap(userId);
    }

    @PutMapping("/progress")
    public RoadmapItemResponse updateProgress(@Valid @RequestBody ProgressUpdateRequest request) {
        return navigatorService.updateProgress(request);
    }

    @PutMapping("/preferences")
    public RoadmapItemResponse updatePreferences(@Valid @RequestBody RoadmapPreferenceUpdateRequest request) {
        return navigatorService.updateRoadmapPreferences(request);
    }
}
