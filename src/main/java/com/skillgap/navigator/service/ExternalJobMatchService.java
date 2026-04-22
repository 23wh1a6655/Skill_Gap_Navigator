package com.skillgap.navigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgap.navigator.dto.JobMatchResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ExternalJobMatchService {

    private static final String ARBEITNOW_API = "https://arbeitnow.com/api/job-board-api";
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<JobMatchResponse> matchJobs(String roleName, Set<String> strongSkills, Set<String> missingSkills) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ARBEITNOW_API))
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode jobs = root.path("data");
            if (!jobs.isArray()) {
                return List.of();
            }

            List<JobMatchResponse> matches = new ArrayList<>();
            String roleLower = roleName.toLowerCase(Locale.ROOT);
            for (JsonNode job : jobs) {
                String title = job.path("title").asText("");
                String description = job.path("description").asText("");
                String combined = (title + " " + description + " " + job.path("company_name").asText("")).toLowerCase(Locale.ROOT);
                if (!combined.contains(roleLower.split(" ")[0])) {
                    continue;
                }

                List<String> matched = strongSkills.stream()
                        .filter(skill -> combined.contains(skill.toLowerCase(Locale.ROOT)))
                        .sorted()
                        .toList();
                List<String> missing = missingSkills.stream()
                        .filter(skill -> combined.contains(skill.toLowerCase(Locale.ROOT)))
                        .sorted()
                        .toList();
                int score = Math.max(10, Math.min(100, matched.size() * 20 - missing.size() * 10 + 40));

                matches.add(new JobMatchResponse(
                        title,
                        job.path("company_name").asText("Unknown company"),
                        job.path("location").asText("Remote"),
                        job.path("url").asText(""),
                        score,
                        matched,
                        missing,
                        "Arbeitnow"));
                if (matches.size() >= 6) {
                    break;
                }
            }

            return matches.stream()
                    .sorted(Comparator.comparingInt(JobMatchResponse::matchScore).reversed())
                    .toList();
        } catch (IOException | InterruptedException ignored) {
            return List.of();
        }
    }
}
