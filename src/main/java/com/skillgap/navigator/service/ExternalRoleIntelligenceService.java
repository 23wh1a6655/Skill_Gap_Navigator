package com.skillgap.navigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgap.navigator.dto.RoleResponse;
import com.skillgap.navigator.dto.SkillRequirementResponse;
import com.skillgap.navigator.entity.SkillLevel;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ExternalRoleIntelligenceService {

    private static final String ESCO_API = "https://ec.europa.eu/esco/api";
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RoleResponse> searchRoles(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            URI uri = URI.create(ESCO_API + "/search?text=" + encoded + "&type=occupation&language=en&limit=8");
            JsonNode root = getJson(uri);
            JsonNode results = root.path("_embedded").path("results");
            if (!results.isArray()) {
                return List.of();
            }

            List<RoleResponse> matches = new ArrayList<>();
            for (JsonNode item : results) {
                String title = item.path("title").asText("");
                if (title.isBlank()) {
                    continue;
                }
                String description = item.path("className").asText("Live occupation match from ESCO");
                matches.add(new RoleResponse(
                        title,
                        description,
                        "Live market taxonomy result",
                        "Fetched from ESCO occupation search",
                        List.of("Live role lookup", "Dynamic skill discovery", "Assessment-driven roadmap"),
                        List.of()));
            }
            return matches;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public Optional<RoleResponse> previewRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Optional.empty();
        }
        try {
            String encoded = URLEncoder.encode(roleName.trim(), StandardCharsets.UTF_8);
            URI searchUri = URI.create(ESCO_API + "/search?text=" + encoded + "&type=occupation&language=en&limit=1");
            JsonNode searchRoot = getJson(searchUri);
            JsonNode results = searchRoot.path("_embedded").path("results");
            if (!results.isArray() || results.isEmpty()) {
                return Optional.empty();
            }

            JsonNode match = results.get(0);
            String title = match.path("title").asText(roleName.trim());
            String resourceUri = match.path("uri").asText("");
            if (resourceUri.isBlank()) {
                return Optional.of(new RoleResponse(
                        title,
                        "Live occupation match from ESCO",
                        "Live market taxonomy result",
                        "Fetched from ESCO occupation search",
                        List.of("Live role lookup", "Dynamic skill discovery"),
                        List.of()));
            }

            URI occupationUri = URI.create(ESCO_API + "/resource/occupation?uri=" + URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "&language=en");
            JsonNode occupation = getJson(occupationUri);
            List<SkillRequirementResponse> skills = new ArrayList<>();
            JsonNode essentialSkills = occupation.path("_links").path("hasEssentialSkill");
            if (essentialSkills.isArray()) {
                for (JsonNode skill : essentialSkills) {
                    String skillTitle = skill.path("title").asText("");
                    if (skillTitle.isBlank()) {
                        continue;
                    }
                    skills.add(new SkillRequirementResponse(
                            skillTitle,
                            SkillLevel.INTERMEDIATE,
                            null,
                            "Live Skill",
                            "Fetched from ESCO essential skills for " + title,
                            false));
                    if (skills.size() >= 10) {
                        break;
                    }
                }
            }

            if (skills.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new RoleResponse(
                    title,
                    "Live occupation preview fetched from the ESCO skills taxonomy.",
                    "Live market taxonomy result",
                    "Fetched from ESCO essential skills",
                    List.of("Live role lookup", "Dynamic skill discovery", "API-driven assessment"),
                    skills));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private JsonNode getJson(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("External API returned status " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }
}
