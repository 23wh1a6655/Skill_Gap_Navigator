package com.skillgap.navigator.service;

import com.skillgap.navigator.dto.GuidedLearningStepResponse;
import com.skillgap.navigator.dto.LearningResourceResponse;
import com.skillgap.navigator.dto.MockInterviewQuestionResponse;
import com.skillgap.navigator.dto.PracticeProjectResponse;
import com.skillgap.navigator.dto.RoleResponse;
import com.skillgap.navigator.dto.SkillRequirementResponse;
import com.skillgap.navigator.entity.SkillLevel;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final ExternalRoleIntelligenceService externalRoleIntelligenceService;

    private final Map<String, SkillBlueprint> skillCatalog = createSkillCatalog();
    private final Map<String, RoleDefinition> presetRoles = createPresetRoles();
    private final Map<String, List<String>> roleKeywords = createRoleKeywords();

    public CatalogService(ExternalRoleIntelligenceService externalRoleIntelligenceService) {
        this.externalRoleIntelligenceService = externalRoleIntelligenceService;
    }

    public List<RoleResponse> getRoles() {
        return presetRoles.values().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    public List<RoleResponse> searchRoles(String query) {
        List<RoleResponse> liveRoles = externalRoleIntelligenceService.searchRoles(query);
        if (!liveRoles.isEmpty()) {
            return liveRoles;
        }
        return presetRoles.values().stream()
                .map(this::toRoleResponse)
                .filter(role -> query == null || query.isBlank() || role.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
                .toList();
    }

    public Optional<RoleDefinition> findRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Optional.empty();
        }
        RoleDefinition preset = presetRoles.get(normalizeTitle(roleName));
        if (preset != null) {
            return Optional.of(preset);
        }
        return Optional.of(buildDynamicRole(roleName));
    }

    public List<LearningResourceResponse> getResources(String skillName) {
        SkillBlueprint blueprint = skillCatalog.get(skillName);
        if (blueprint == null) {
            String query = skillName.replace(" ", "+");
            return List.of(
                    resource(skillName + " Fundamentals", "YouTube", "Video Course", "2h", "https://www.youtube.com/results?search_query=" + query + "+course", "Start with a broad course to build the basics.", "Beginner", "English", "Free", 4.6, 88, 1, "Best starting point for fundamentals."),
                    resource(skillName + " Learning Path", "Google", "Reading", "Self-paced", "https://www.google.com/search?q=" + query + "+learning+path", "Use a structured learning path to deepen this skill.", "Intermediate", "English", "Free", 4.5, 83, 2, "Good for deeper structured learning."),
                    resource(skillName + " Practice Projects", "Google", "Practice", "Self-paced", "https://www.google.com/search?q=" + query + "+project+ideas", "Reinforce the topic with hands-on exercises.", "Intermediate", "English", "Free", 4.7, 95, 3, "Most practical for job-ready progress."));
        }
        return blueprint.resources().stream().sorted(Comparator.comparingInt(LearningResourceResponse::rank)).toList();
    }

    public List<QuizQuestion> getQuizQuestions(String skillName) {
        return getQuizQuestions(skillName, SkillLevel.BEGINNER, SkillLevel.INTERMEDIATE);
    }

    public List<QuizQuestion> getQuizQuestions(String skillName, SkillLevel currentLevel, SkillLevel targetLevel) {
        SkillBlueprint blueprint = skillCatalog.get(skillName);
        String category = blueprint != null ? blueprint.category() : "Professional";
        List<String> prerequisites = getPrerequisites(skillName);
        return List.of(
                new QuizQuestion(
                        slug(skillName) + "-1",
                        "Which option is the best description of " + skillName + "?",
                        List.of(
                                "A core " + category.toLowerCase(Locale.ROOT) + " skill used in real projects",
                                "A hardware device only",
                                "A social media platform",
                                "A database table"),
                        "A core " + category.toLowerCase(Locale.ROOT) + " skill used in real projects",
                        skillName + " is a practical skill area that is applied in real work.",
                        "MCQ",
                        targetLevel == SkillLevel.ADVANCED ? "Advanced" : "Intermediate",
                        skillName + " fundamentals",
                        List.of("Recognize the purpose of the skill", "Relate it to real work output")),
                new QuizQuestion(
                        slug(skillName) + "-2",
                        "What is the best way to improve " + skillName + "?",
                        List.of(
                                "Combine guided learning with projects",
                                "Only memorize definitions",
                                "Avoid practice",
                                "Skip feedback"),
                        "Combine guided learning with projects",
                        "Structured learning plus hands-on practice is the strongest improvement path.",
                        "Scenario",
                        targetLevel == SkillLevel.ADVANCED ? "Advanced" : "Intermediate",
                        "learning strategy",
                        List.of("Choose an action-oriented plan", "Show practical learning judgment")),
                new QuizQuestion(
                        slug(skillName) + "-3",
                        "Which prerequisite is most important before advancing in " + skillName + "?",
                        List.of(
                                prerequisites.get(0),
                                "Avoiding revision",
                                "Skipping practice",
                                "Only watching random videos"),
                        prerequisites.get(0),
                        "Strong progress in " + skillName + " depends on mastering the right foundations first.",
                        "MCQ",
                        "Intermediate",
                        "prerequisites",
                        List.of("Recognize foundational skills", "Connect progression to prerequisites")),
                new QuizQuestion(
                        slug(skillName) + "-4",
                        "Describe how you would prove " + skillName + " in a portfolio or practical project.",
                        List.of(),
                        null,
                        "A strong answer should mention a real task, implementation steps, and measurable output.",
                        "Assignment",
                        targetLevel == SkillLevel.ADVANCED ? "Advanced" : "Intermediate",
                        "portfolio proof",
                        List.of("Mention a practical task", "Explain implementation", "Describe measurable outcome")),
                new QuizQuestion(
                        slug(skillName) + "-5",
                        "Describe how you would apply " + skillName + " in a small role-relevant project.",
                        List.of(),
                        null,
                        "A strong answer should explain the task, the implementation approach, and the expected outcome.",
                        "Assignment",
                        targetLevel == SkillLevel.ADVANCED ? "Advanced" : "Intermediate",
                        "project thinking",
                        List.of("Mention a practical task", "Explain implementation", "Describe expected outcome")));
    }

    public PracticeProjectResponse getMiniProject(String skillName) {
        SkillBlueprint blueprint = skillCatalog.get(skillName);
        return blueprint != null ? blueprint.miniProject() : defaultMiniProject(skillName);
    }

    public PracticeProjectResponse getPortfolioProject(String skillName) {
        SkillBlueprint blueprint = skillCatalog.get(skillName);
        return blueprint != null ? blueprint.portfolioProject() : defaultPortfolioProject(skillName);
    }

    public List<GuidedLearningStepResponse> getGuidedLearningPath(String skillName, SkillLevel currentLevel, SkillLevel targetLevel) {
        return List.of(
                new GuidedLearningStepResponse(1, "Learn Foundations", "Video", "Understand the basics of " + skillName, "Finish one guided lesson."),
                new GuidedLearningStepResponse(2, "Read Documentation", "Docs", "Review official concepts and syntax", "Capture your key notes."),
                new GuidedLearningStepResponse(3, "Practice", "Practice", "Apply the topic in a small exercise", "Complete one focused exercise."),
                new GuidedLearningStepResponse(4, "Validate Understanding", "Quiz", "Test your understanding for " + targetLevel + " level", "Identify weak areas to revise."),
                new GuidedLearningStepResponse(5, "Build a Project", "Project", "Create something practical with " + skillName, "Produce a demo or portfolio artifact."));
    }

    public List<String> getPrerequisites(String skillName) {
        SkillBlueprint blueprint = skillCatalog.get(skillName);
        return blueprint != null ? blueprint.prerequisites() : List.of("Problem Solving");
    }

    public List<MockInterviewQuestionResponse> getMockInterviewQuestions(String roleName, String skillName) {
        SkillBlueprint blueprint = skillCatalog.get(skillName);
        if (blueprint != null) {
            return blueprint.interviewQuestions();
        }
        return List.of(
                new MockInterviewQuestionResponse(slug(skillName) + "-interview-1", "How would you use " + skillName + " in a " + roleName + " project?", skillName, List.of("Explain context", "Describe implementation", "Mention outcome")),
                new MockInterviewQuestionResponse(slug(skillName) + "-interview-2", "What common mistakes should a learner avoid while applying " + skillName + "?", "Decision making", List.of("Show awareness of pitfalls", "Explain how to avoid them")));
    }

    public RoleResponse toRoleResponse(RoleDefinition definition) {
        return new RoleResponse(
                definition.name(),
                definition.description(),
                definition.salarySignal(),
                definition.hiringSignal(),
                definition.outcomes(),
                definition.skills().stream()
                        .map(skill -> new SkillRequirementResponse(skill.skillName(), skill.targetLevel(), null, skill.category(), skill.description(), false))
                        .toList());
    }

    public Optional<RoleResponse> previewRoleLive(String roleName) {
        return externalRoleIntelligenceService.previewRole(roleName);
    }

    private RoleDefinition buildDynamicRole(String rawRoleName) {
        String roleName = normalizeTitle(rawRoleName);
        String lowerRole = rawRoleName.toLowerCase(Locale.ROOT);

        List<String> chosenSkills = roleKeywords.entrySet().stream()
                .filter(entry -> entry.getKey().equals("default") || lowerRole.contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .distinct()
                .limit(8)
                .toList();

        if (chosenSkills.isEmpty()) {
            chosenSkills = roleKeywords.get("default");
        }

        List<SkillRequirement> skills = chosenSkills.stream()
                .map(skillName -> {
                    SkillBlueprint blueprint = skillCatalog.get(skillName);
                    if (blueprint == null) {
                        return new SkillRequirement(skillName, SkillLevel.INTERMEDIATE, "General", "Important for performing well in this role.");
                    }
                    return new SkillRequirement(skillName, blueprint.targetLevel(), blueprint.category(), blueprint.description());
                })
                .sorted(Comparator.comparingInt(skill -> getPrerequisites(skill.skillName()).size()))
                .toList();

        return new RoleDefinition(
                roleName,
                "A generated roadmap for " + roleName + " based on common industry expectations and a reusable skill library.",
                "Varies by company and market level",
                "Generated from role title keywords",
                List.of("Build your missing skills", "Track progress with quizzes", "Create a role-ready learning path"),
                skills);
    }

    private String normalizeTitle(String roleName) {
        return Arrays.stream(roleName.trim().split("\\s+"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replace(" ", "-");
    }

    private Map<String, SkillBlueprint> createSkillCatalog() {
        Map<String, SkillBlueprint> catalog = new LinkedHashMap<>();
        catalog.put("HTML", skill("HTML", "Frontend", SkillLevel.ADVANCED, "Semantic structure, accessibility, and content layout."));
        catalog.put("CSS", skill("CSS", "Frontend", SkillLevel.ADVANCED, "Responsive layout systems, spacing, and visual polish."));
        catalog.put("JavaScript", skill("JavaScript", "Frontend", SkillLevel.ADVANCED, "Dynamic behavior, async flows, and application logic."));
        catalog.put("TypeScript", skill("TypeScript", "Frontend", SkillLevel.INTERMEDIATE, "Safer application code and stronger maintainability."));
        catalog.put("React", skill("React", "Frontend", SkillLevel.INTERMEDIATE, "Component-driven UI architecture for modern web apps."));
        catalog.put("UI Design", skill("UI Design", "Product", SkillLevel.INTERMEDIATE, "Hierarchy, spacing, visual language, and interface quality."));
        catalog.put("Java", skill("Java", "Backend", SkillLevel.ADVANCED, "Strong foundation for enterprise backend development."));
        catalog.put("Spring Boot", skill("Spring Boot", "Backend", SkillLevel.ADVANCED, "API development, validation, service layers, and data integration."));
        catalog.put("SQL", skill("SQL", "Data", SkillLevel.INTERMEDIATE, "Relational querying, joins, and data analysis."));
        catalog.put("REST API", skill("REST API", "Backend", SkillLevel.INTERMEDIATE, "Designing request flows and reliable service contracts."));
        catalog.put("Git", skill("Git", "Collaboration", SkillLevel.BEGINNER, "Version control and team workflow."));
        catalog.put("Docker", skill("Docker", "DevOps", SkillLevel.INTERMEDIATE, "Containerize and ship applications consistently."));
        catalog.put("Linux", skill("Linux", "DevOps", SkillLevel.INTERMEDIATE, "Command line operations and server basics."));
        catalog.put("Kubernetes", skill("Kubernetes", "DevOps", SkillLevel.INTERMEDIATE, "Orchestrate containers at scale."));
        catalog.put("CI/CD", skill("CI/CD", "DevOps", SkillLevel.INTERMEDIATE, "Automated testing and deployment pipelines."));
        catalog.put("Cloud", skill("Cloud", "DevOps", SkillLevel.INTERMEDIATE, "Deploy and manage services in cloud environments."));
        catalog.put("Python", skill("Python", "Data", SkillLevel.ADVANCED, "Widely used for analytics, automation, and machine learning."));
        catalog.put("Statistics", skill("Statistics", "Data", SkillLevel.INTERMEDIATE, "Data reasoning and experiment interpretation."));
        catalog.put("Data Visualization", skill("Data Visualization", "Data", SkillLevel.INTERMEDIATE, "Turn data into charts and business insights."));
        catalog.put("Excel", skill("Excel", "Data", SkillLevel.INTERMEDIATE, "Fast analysis and reporting for business workflows."));
        catalog.put("Machine Learning", skill("Machine Learning", "AI", SkillLevel.INTERMEDIATE, "Modeling, prediction, and evaluation basics."));
        catalog.put("Deep Learning", skill("Deep Learning", "AI", SkillLevel.INTERMEDIATE, "Neural network fundamentals for advanced AI work."));
        catalog.put("MLOps", skill("MLOps", "AI", SkillLevel.BEGINNER, "Deploy, monitor, and maintain machine learning systems."));
        catalog.put("Kotlin", skill("Kotlin", "Mobile", SkillLevel.INTERMEDIATE, "Modern Android application development."));
        catalog.put("Android", skill("Android", "Mobile", SkillLevel.INTERMEDIATE, "Build and maintain native Android applications."));
        catalog.put("Firebase", skill("Firebase", "Mobile", SkillLevel.BEGINNER, "Backend services for authentication, storage, and analytics."));
        catalog.put("Testing", skill("Testing", "QA", SkillLevel.INTERMEDIATE, "Manual testing strategy and quality thinking."));
        catalog.put("Automation Testing", skill("Automation Testing", "QA", SkillLevel.INTERMEDIATE, "Automate repeatable test cases."));
        catalog.put("API Testing", skill("API Testing", "QA", SkillLevel.INTERMEDIATE, "Validate backend flows and service behavior."));
        catalog.put("Selenium", skill("Selenium", "QA", SkillLevel.BEGINNER, "Web UI automation testing."));
        catalog.put("System Design", skill("System Design", "Architecture", SkillLevel.BEGINNER, "Understand scalability, tradeoffs, and component boundaries."));
        catalog.put("Communication", skill("Communication", "Professional", SkillLevel.INTERMEDIATE, "Explain work clearly and collaborate effectively."));
        catalog.put("Problem Solving", skill("Problem Solving", "Professional", SkillLevel.ADVANCED, "Break down requirements and solve ambiguity."));
        catalog.put("Figma", skill("Figma", "Design", SkillLevel.INTERMEDIATE, "UI design and collaboration for product teams."));
        catalog.put("UX Research", skill("UX Research", "Design", SkillLevel.BEGINNER, "Understand user needs and improve experience quality."));
        catalog.put("Networking", skill("Networking", "Security", SkillLevel.INTERMEDIATE, "Protocols, routing, and infrastructure basics."));
        catalog.put("Cybersecurity", skill("Cybersecurity", "Security", SkillLevel.INTERMEDIATE, "Protect systems, users, and application flows."));
        catalog.put("Incident Response", skill("Incident Response", "Security", SkillLevel.BEGINNER, "Identify, contain, and document security events."));
        return catalog;
    }

    private Map<String, RoleDefinition> createPresetRoles() {
        Map<String, RoleDefinition> roles = new LinkedHashMap<>();
        roles.put("Frontend Developer", role("Frontend Developer", List.of("Ship polished UI", "Build reusable interfaces", "Work closely with design"), "High demand in product teams", "Strong in web product companies", List.of("HTML", "CSS", "JavaScript", "TypeScript", "React", "UI Design", "Git")));
        roles.put("Backend Developer", role("Backend Developer", List.of("Build APIs", "Design services", "Handle data safely"), "Strong enterprise demand", "Common in Java and cloud teams", List.of("Java", "Spring Boot", "SQL", "REST API", "Git", "Docker", "System Design")));
        roles.put("Full Stack Developer", role("Full Stack Developer", List.of("Own features end-to-end", "Build UI and APIs", "Ship fast"), "Very versatile role", "Popular in startups and product teams", List.of("HTML", "CSS", "JavaScript", "React", "Spring Boot", "SQL", "Git")));
        roles.put("Data Analyst", role("Data Analyst", List.of("Analyze business data", "Build reports", "Present insights"), "Widely needed in business teams", "Popular in analytics hiring", List.of("SQL", "Excel", "Python", "Statistics", "Data Visualization", "Communication")));
        roles.put("Data Scientist", role("Data Scientist", List.of("Build predictive models", "Work with experiments", "Communicate findings"), "Strong AI and analytics demand", "Common in data-first companies", List.of("Python", "Statistics", "Machine Learning", "SQL", "Data Visualization", "Communication")));
        roles.put("DevOps Engineer", role("DevOps Engineer", List.of("Automate deployments", "Improve reliability", "Support delivery velocity"), "High leverage infrastructure role", "Strong in cloud-native teams", List.of("Linux", "Docker", "Kubernetes", "CI/CD", "Cloud", "Git")));
        roles.put("Mobile App Developer", role("Mobile App Developer", List.of("Build mobile features", "Ship native experiences", "Integrate backend services"), "Growing mobile demand", "Strong in consumer apps", List.of("Java", "Kotlin", "Android", "Firebase", "REST API", "Git", "UI Design")));
        roles.put("QA Engineer", role("QA Engineer", List.of("Protect release quality", "Find regressions", "Automate checks"), "Important for stable delivery", "Common in product engineering teams", List.of("Testing", "Automation Testing", "API Testing", "Selenium", "SQL", "Communication")));
        roles.put("UI UX Designer", role("UI UX Designer", List.of("Design product flows", "Prototype ideas", "Improve usability"), "Strong product design need", "Common in digital product teams", List.of("UI Design", "Figma", "UX Research", "Communication", "Problem Solving")));
        roles.put("Cybersecurity Analyst", role("Cybersecurity Analyst", List.of("Protect systems", "Monitor threats", "Respond to incidents"), "Strong security demand", "Growing across enterprises", List.of("Networking", "Linux", "Cybersecurity", "Python", "Incident Response", "Communication")));
        roles.put("Machine Learning Engineer", role("Machine Learning Engineer", List.of("Train and deploy models", "Build AI systems", "Operationalize ML"), "Strong AI platform demand", "Popular in modern AI teams", List.of("Python", "Machine Learning", "Deep Learning", "SQL", "MLOps", "Git")));
        roles.put("Cloud Engineer", role("Cloud Engineer", List.of("Deploy scalable services", "Manage infrastructure", "Optimize platform reliability"), "Growing cloud adoption", "Common in platform teams", List.of("Cloud", "Linux", "Docker", "CI/CD", "Git", "System Design")));
        return roles;
    }

    private Map<String, List<String>> createRoleKeywords() {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("frontend", List.of("HTML", "CSS", "JavaScript", "TypeScript", "React", "UI Design", "Git"));
        keywords.put("backend", List.of("Java", "Spring Boot", "SQL", "REST API", "Docker", "Git", "System Design"));
        keywords.put("full stack", List.of("HTML", "CSS", "JavaScript", "React", "Spring Boot", "SQL", "Git"));
        keywords.put("data", List.of("SQL", "Python", "Statistics", "Data Visualization", "Excel", "Communication"));
        keywords.put("scientist", List.of("Python", "Statistics", "Machine Learning", "SQL", "Data Visualization", "Communication"));
        keywords.put("machine learning", List.of("Python", "Machine Learning", "Deep Learning", "SQL", "MLOps", "Git"));
        keywords.put("ai", List.of("Python", "Machine Learning", "Deep Learning", "MLOps", "Git", "Problem Solving"));
        keywords.put("devops", List.of("Linux", "Docker", "Kubernetes", "CI/CD", "Cloud", "Git"));
        keywords.put("cloud", List.of("Cloud", "Linux", "Docker", "CI/CD", "Git", "System Design"));
        keywords.put("mobile", List.of("Java", "Kotlin", "Android", "Firebase", "REST API", "Git"));
        keywords.put("android", List.of("Java", "Kotlin", "Android", "Firebase", "REST API", "Git"));
        keywords.put("qa", List.of("Testing", "Automation Testing", "API Testing", "Selenium", "SQL", "Communication"));
        keywords.put("test", List.of("Testing", "Automation Testing", "API Testing", "Selenium", "SQL", "Communication"));
        keywords.put("design", List.of("UI Design", "Figma", "UX Research", "Communication", "Problem Solving"));
        keywords.put("security", List.of("Networking", "Linux", "Cybersecurity", "Python", "Incident Response", "Communication"));
        keywords.put("analyst", List.of("SQL", "Excel", "Data Visualization", "Communication", "Problem Solving"));
        keywords.put("default", List.of("Communication", "Problem Solving", "Git", "SQL", "UI Design"));
        return keywords;
    }

    private Map<String, List<QuizQuestion>> createDedicatedQuizzes() {
        Map<String, List<QuizQuestion>> quizzes = new LinkedHashMap<>();
        quizzes.put("React", List.of(
                new QuizQuestion("react-1", "Which hook manages local component state?", List.of("useState", "useFetch", "useStore", "useRouter"), "useState", "useState is the standard hook for local state.", "MCQ", "Intermediate", "state management", List.of("Know the correct hook", "Relate it to component state")),
                new QuizQuestion("react-2", "Why do React lists need stable keys?", List.of("To improve styling", "To help React track item changes", "To remove props", "To avoid JSX"), "To help React track item changes", "Stable keys help React reconcile updates correctly.", "Scenario", "Intermediate", "reconciliation", List.of("Understand the purpose of keys", "Connect it to UI update behavior")),
                new QuizQuestion("react-3", "Describe a small UI project where you would use reusable components.", List.of(), null, "A strong answer should mention reuse, props, and maintainability.", "Assignment", "Advanced", "component architecture", List.of("Mention component reuse", "Explain a practical UI case"))));
        quizzes.put("Spring Boot", List.of(
                new QuizQuestion("spring-1", "Which annotation exposes REST endpoints?", List.of("@RestController", "@Service", "@Entity", "@Configuration"), "@RestController", "REST controllers handle HTTP endpoints.", "MCQ", "Intermediate", "web layer", List.of("Identify correct annotation", "Know the controller responsibility")),
                new QuizQuestion("spring-2", "Where is business logic usually placed?", List.of("Service", "Properties", "Entity only", "Static folder"), "Service", "Business logic typically belongs in the service layer.", "Scenario", "Intermediate", "service layer", List.of("Choose the correct application layer", "Relate architecture to maintainability")),
                new QuizQuestion("spring-3", "Describe how you would build a simple API using Spring Boot layers.", List.of(), null, "A strong answer mentions controller, service, validation, and persistence.", "Assignment", "Advanced", "API design", List.of("Mention layered structure", "Include validation or persistence"))));
        quizzes.put("SQL", List.of(
                new QuizQuestion("sql-1", "Which clause filters grouped results?", List.of("WHERE", "HAVING", "FROM", "ORDER BY"), "HAVING", "HAVING is used after grouping.", "MCQ", "Intermediate", "aggregations", List.of("Know WHERE vs HAVING", "Understand grouping flow")),
                new QuizQuestion("sql-2", "Which join returns only matching rows?", List.of("INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "CROSS JOIN"), "INNER JOIN", "INNER JOIN keeps matching rows only.", "Scenario", "Intermediate", "joins", List.of("Pick the correct join type", "Relate it to the data requirement")),
                new QuizQuestion("sql-3", "Describe a business report query you would build with joins and grouping.", List.of(), null, "A strong answer includes tables, grouping logic, and expected output.", "Assignment", "Advanced", "query design", List.of("Mention joins", "Mention grouping", "Describe useful output"))));
        quizzes.put("JavaScript", List.of(
                new QuizQuestion("js-1", "Which keyword creates a block-scoped variable?", List.of("let", "var", "scope", "block"), "let", "let is block-scoped.", "MCQ", "Intermediate", "variables", List.of("Differentiate let vs var", "Know block scope")),
                new QuizQuestion("js-2", "What does async/await improve?", List.of("Readable asynchronous code", "CSS performance", "Database indexing", "HTML semantics"), "Readable asynchronous code", "async/await makes promise-based code easier to follow.", "Scenario", "Intermediate", "asynchronous code", List.of("Relate async/await to readability", "Understand promise-based flow")),
                new QuizQuestion("js-3", "Describe a feature where you would use map to transform data for UI rendering.", List.of(), null, "A strong answer should connect data transformation to a practical UI use case.", "Assignment", "Advanced", "array transformation", List.of("Mention input and output arrays", "Connect transformation to a real feature"))));
        return quizzes;
    }

    private RoleDefinition role(String name, List<String> outcomes, String salarySignal, String hiringSignal, List<String> skills) {
        List<SkillRequirement> requirements = skills.stream()
                .map(skillName -> {
                    SkillBlueprint blueprint = skillCatalog.get(skillName);
                    return new SkillRequirement(skillName, blueprint.targetLevel(), blueprint.category(), blueprint.description());
                })
                .sorted(Comparator.comparingInt(skill -> getPrerequisites(skill.skillName()).size()))
                .toList();
        return new RoleDefinition(
                name,
                "A curated learning track for " + name + " with guided sequencing, practical projects, and week-by-week goals.",
                salarySignal,
                hiringSignal,
                outcomes,
                requirements);
    }

    private SkillBlueprint skill(String name, String category, SkillLevel targetLevel, String description) {
        String query = name.replace(" ", "+");
        return new SkillBlueprint(
                name,
                category,
                targetLevel,
                description,
                defaultPrerequisites(name),
                defaultMiniProject(name),
                defaultPortfolioProject(name),
                defaultInterviewQuestions(name),
                List.of(
                        resource(name + " Course", "YouTube", "Video Course", "2-6h", "https://www.youtube.com/results?search_query=" + query + "+course", "Build foundations with a guided course.", "Beginner", "English", "Free", 4.7, 86, 1, "Best first step for structured learning."),
                        resource(name + " Documentation", "Google", "Documentation", "Self-paced", "https://www.google.com/search?q=" + query + "+documentation", "Use references while practicing.", "Intermediate", "English", "Free", 4.6, 82, 2, "Useful for depth and reference."),
                        resource(name + " Projects", "Google", "Practice", "Self-paced", "https://www.google.com/search?q=" + query + "+project+ideas", "Reinforce learning with projects and exercises.", "Intermediate", "English", "Free", 4.8, 95, 3, "Most practical route toward job readiness.")));
    }

    private LearningResourceResponse resource(String title, String platform, String type, String duration, String url, String description, String level, String language, String budget, double rating, int practicalScore, int rank, String recommendationReason) {
        return new LearningResourceResponse(title, platform, type, duration, url, description, level, language, budget, rating, practicalScore, rank, recommendationReason);
    }

    private List<String> defaultPrerequisites(String name) {
        return switch (name) {
            case "CSS" -> List.of("HTML");
            case "JavaScript" -> List.of("HTML", "CSS");
            case "TypeScript" -> List.of("JavaScript");
            case "React" -> List.of("HTML", "CSS", "JavaScript");
            case "Spring Boot" -> List.of("Java", "REST API");
            case "Docker" -> List.of("Linux");
            case "Kubernetes" -> List.of("Docker", "Cloud");
            case "Machine Learning" -> List.of("Python", "Statistics");
            case "Deep Learning" -> List.of("Machine Learning");
            case "MLOps" -> List.of("Machine Learning", "Cloud");
            default -> List.of("Problem Solving");
        };
    }

    private PracticeProjectResponse defaultMiniProject(String skillName) {
        return new PracticeProjectResponse(skillName + " Mini Project", "Mini Project", "Intermediate", "1 week", "Practice " + skillName + " through a focused hands-on exercise.", List.of("Working solution", "Short summary", "Demo or screenshot"));
    }

    private PracticeProjectResponse defaultPortfolioProject(String skillName) {
        return new PracticeProjectResponse(skillName + " Portfolio Project", "Portfolio Project", "Advanced", "2-3 weeks", "Build a portfolio-ready artifact proving your skill in " + skillName + ".", List.of("Project source", "README", "Demo link or screenshots"));
    }

    private List<MockInterviewQuestionResponse> defaultInterviewQuestions(String skillName) {
        return List.of(
                new MockInterviewQuestionResponse(slug(skillName) + "-interview-1", "Explain how you would use " + skillName + " in a real project.", skillName, List.of("Explain context", "Describe implementation", "Mention measurable outcome")),
                new MockInterviewQuestionResponse(slug(skillName) + "-interview-2", "What tradeoffs or common mistakes matter when working with " + skillName + "?", "Decision making", List.of("Show awareness of pitfalls", "Discuss tradeoffs", "Keep the answer practical")));
    }

    public record RoleDefinition(String name, String description, String salarySignal, String hiringSignal, List<String> outcomes, List<SkillRequirement> skills) {}

    public record SkillRequirement(String skillName, SkillLevel targetLevel, String category, String description) {}

    public record SkillBlueprint(
            String name,
            String category,
            SkillLevel targetLevel,
            String description,
            List<String> prerequisites,
            PracticeProjectResponse miniProject,
            PracticeProjectResponse portfolioProject,
            List<MockInterviewQuestionResponse> interviewQuestions,
            List<LearningResourceResponse> resources) {}

    public record QuizQuestion(
            String id,
            String prompt,
            List<String> options,
            String answer,
            String explanation,
            String questionType,
            String difficulty,
            String concept,
            List<String> evaluationCriteria) {}
}
