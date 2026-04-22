package com.skillgap.navigator.dto;

public record DailyStudyTaskResponse(
        int day,
        String title,
        String objective,
        String duration) {
}
