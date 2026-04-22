package com.skillgap.navigator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class NavigatorApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerAndLoginWithNewUserWorks() throws Exception {
        long seed = System.currentTimeMillis();
        String username = "User" + seed;
        String email = "user" + seed + "@mail.com";
        String password = "secret123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void roadmapGenerationFlowWorks() throws Exception {
        long seed = System.currentTimeMillis();
        String username = "RoadmapUser" + seed;
        String email = "roadmap" + seed + "@mail.com";
        String password = "secret123";

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        long userId = registerJson.path("user").path("id").asLong();
        String token = registerJson.path("token").asText();

        mockMvc.perform(post("/api/skills/analysis")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "role": "Backend Developer",
                                  "weeklyHours": 8,
                                  "targetWeeks": 6,
                                  "skills": [
                                    { "skillName": "Java", "level": "BEGINNER" },
                                    { "skillName": "SQL", "level": "BEGINNER" }
                                  ]
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Backend Developer"))
                .andExpect(jsonPath("$.estimatedWeeks").isNumber())
                .andExpect(jsonPath("$.milestones").isArray());

        mockMvc.perform(get("/api/roadmap/{userId}", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skillName").isNotEmpty())
                .andExpect(jsonPath("$[0].resources").isArray())
                .andExpect(jsonPath("$[0].guidedPath").isArray());
    }

    @Test
    void protectedEndpointsReturnJsonWhenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/skills/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "role": "Backend Developer",
                                  "skills": []
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please login again."));
    }

    @Test
    void quizAndRoadmapPreferencesFlowWorks() throws Exception {
        long seed = System.currentTimeMillis();
        String username = "FlowUser" + seed;
        String email = "flow" + seed + "@mail.com";
        String password = "secret123";

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        long userId = registerJson.path("user").path("id").asLong();
        String token = registerJson.path("token").asText();

        mockMvc.perform(post("/api/skills/analysis")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "role": "Backend Developer",
                                  "weeklyHours": 10,
                                  "skills": [
                                    { "skillName": "Java", "level": "BEGINNER" },
                                    { "skillName": "Spring Boot", "level": "BEGINNER" },
                                    { "skillName": "SQL", "level": "BEGINNER" }
                                  ]
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/roadmap/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "skillName": "Java",
                                  "notes": "Focus on collections and streams.",
                                  "bookmarked": true,
                                  "confidenceScore": 55
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Focus on collections and streams."))
                .andExpect(jsonPath("$.bookmarked").value(true));

        mockMvc.perform(get("/api/quiz")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", String.valueOf(userId))
                        .param("skill", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isArray());

        mockMvc.perform(post("/api/quiz/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "skillName": "Java",
                                  "confidenceRating": 60,
                                  "answers": [
                                    { "questionId": "java-core-1", "selectedOption": "Class", "responseText": "Class" },
                                    { "questionId": "java-core-2", "selectedOption": "int, double, char", "responseText": "int, double, char" },
                                    { "questionId": "java-core-3", "selectedOption": "The method signature and return type", "responseText": "The method signature and return type" }
                                  ]
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentage").isNumber())
                .andExpect(jsonPath("$.feedback").isArray());
    }
}
