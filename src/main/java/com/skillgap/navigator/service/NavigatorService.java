package com.skillgap.navigator.service;

import com.skillgap.navigator.dto.*;
import com.skillgap.navigator.entity.*;
import com.skillgap.navigator.repository.AchievementRepository;
import com.skillgap.navigator.repository.LearningProgressRepository;
import com.skillgap.navigator.repository.QuizAttemptRepository;
import com.skillgap.navigator.repository.UserRepository;
import com.skillgap.navigator.repository.UserSkillRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class NavigatorService {

    private final CatalogService catalogService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AchievementRepository achievementRepository;
    private final ExternalJobMatchService externalJobMatchService;

    public NavigatorService(
            CatalogService catalogService,
            AuthService authService,
            UserRepository userRepository,
            UserSkillRepository userSkillRepository,
            LearningProgressRepository learningProgressRepository,
            QuizAttemptRepository quizAttemptRepository,
            AchievementRepository achievementRepository,
            ExternalJobMatchService externalJobMatchService) {
        this.catalogService = catalogService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.learningProgressRepository = learningProgressRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.achievementRepository = achievementRepository;
        this.externalJobMatchService = externalJobMatchService;
    }

    @Transactional
    public SkillAssessmentResponse analyzeSkills(SkillAssessmentRequest request) {
        var user = authService.getUser(request.userId());
        var roleDefinition = catalogService.findRole(request.role())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Selected role was not found."));

        Map<String, SkillLevel> submittedSkills = new HashMap<>();
        if (request.skills() != null) {
            request.skills().forEach(skill -> submittedSkills.put(skill.skillName(), skill.level()));
        }

        return buildSkillAssessment(user, roleDefinition, submittedSkills, request.weeklyHours());
    }

    public RoleAssessmentPayloadResponse getRoleAssessment(Long userId, String roleName) {
        authService.getUser(userId);
        var roleDefinition = catalogService.findRole(roleName)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Selected role was not found."));

        List<RoleAssessmentQuestionResponse> questions = new ArrayList<>();
        for (CatalogService.SkillRequirement requirement : roleDefinition.skills()) {
            List<CatalogService.QuizQuestion> skillQuestions = catalogService
                    .getQuizQuestions(requirement.skillName(), SkillLevel.BEGINNER, requirement.targetLevel())
                    .stream()
                    .limit(2)
                    .toList();
            for (CatalogService.QuizQuestion question : skillQuestions) {
                questions.add(new RoleAssessmentQuestionResponse(
                        question.id(),
                        requirement.skillName(),
                        question.prompt(),
                        question.options(),
                        question.questionType(),
                        question.difficulty(),
                        question.concept(),
                        question.evaluationCriteria()));
            }
        }

        return new RoleAssessmentPayloadResponse(roleDefinition.name(), roleDefinition.skills().size(), questions.size(), questions);
    }

    @Transactional
    public RoleAssessmentResultResponse submitRoleAssessment(RoleAssessmentSubmissionRequest request) {
        var user = authService.getUser(request.userId());
        var roleDefinition = catalogService.findRole(request.role())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Selected role was not found."));

        Map<String, String> answers = new HashMap<>();
        if (request.answers() != null) {
            request.answers().forEach(answer -> answers.put(
                    answer.questionId(),
                    answer.selectedOption() != null && !answer.selectedOption().isBlank() ? answer.selectedOption() : answer.responseText()));
        }

        int totalQuestions = 0;
        int score = 0;
        Set<String> focusSkills = new LinkedHashSet<>();
        List<SkillInput> inferredSkillInputs = new ArrayList<>();
        List<SkillRequirementResponse> inferredSkills = new ArrayList<>();

        for (CatalogService.SkillRequirement requirement : roleDefinition.skills()) {
            List<CatalogService.QuizQuestion> skillQuestions = catalogService
                    .getQuizQuestions(requirement.skillName(), SkillLevel.BEGINNER, requirement.targetLevel())
                    .stream()
                    .limit(2)
                    .toList();

            int skillScore = 0;
            for (CatalogService.QuizQuestion question : skillQuestions) {
                totalQuestions++;
                String submittedAnswer = answers.get(question.id());
                boolean correct = question.answer() == null
                        ? submittedAnswer != null && submittedAnswer.trim().length() > 20
                        : question.answer().equals(submittedAnswer);
                if (correct) {
                    score++;
                    skillScore++;
                } else {
                    focusSkills.add(requirement.skillName());
                }
            }

            SkillLevel inferredLevel = inferLevel(skillScore, skillQuestions.size(), requirement.targetLevel());
            inferredSkillInputs.add(new SkillInput(requirement.skillName(), inferredLevel));
            inferredSkills.add(new SkillRequirementResponse(
                    requirement.skillName(),
                    requirement.targetLevel(),
                    inferredLevel,
                    requirement.category(),
                    requirement.description(),
                    levelScore(inferredLevel) < levelScore(requirement.targetLevel())));
        }

        SkillAssessmentResponse analysis = buildSkillAssessment(
                user,
                roleDefinition,
                inferredSkillInputs.stream().collect(HashMap::new, (map, item) -> map.put(item.skillName(), item.level()), HashMap::putAll),
                request.weeklyHours());

        issueRoleReadyBadgeIfEligible(request.userId());

        return new RoleAssessmentResultResponse(
                roleDefinition.name(),
                score,
                totalQuestions,
                totalQuestions == 0 ? 0 : (score * 100) / totalQuestions,
                focusSkills.isEmpty()
                        ? List.of("Role-ready")
                        : focusSkills.stream().map(skill -> "Focus on " + skill).toList(),
                inferredSkills,
                analysis);
    }

    private SkillAssessmentResponse buildSkillAssessment(
            User user,
            CatalogService.RoleDefinition roleDefinition,
            Map<String, SkillLevel> submittedSkills,
            Integer requestedWeeklyHours) {

        user.setRole(roleDefinition.name());
        user.setOnboardingComplete(true);
        userRepository.save(user);

        userSkillRepository.deleteByUserId(user.getId());
        learningProgressRepository.deleteByUserId(user.getId());

        List<SkillRequirementResponse> requiredSkills = new ArrayList<>();
        List<SkillRequirementResponse> missingSkills = new ArrayList<>();
        List<SkillRequirementResponse> strengths = new ArrayList<>();

        int readinessPoints = 0;
        int totalPossiblePoints = 0;
        int displayOrder = 1;
        int weeklyHours = requestedWeeklyHours == null || requestedWeeklyHours < 4 ? 8 : requestedWeeklyHours;

        for (CatalogService.SkillRequirement requirement : roleDefinition.skills()) {
            SkillLevel currentLevel = submittedSkills.getOrDefault(requirement.skillName(), SkillLevel.BEGINNER);
            boolean missing = levelScore(currentLevel) < levelScore(requirement.targetLevel());

            UserSkill userSkill = new UserSkill();
            userSkill.setUserId(user.getId());
            userSkill.setRoleName(roleDefinition.name());
            userSkill.setSkillName(requirement.skillName());
            userSkill.setCurrentLevel(currentLevel);
            userSkill.setTargetLevel(requirement.targetLevel());
            userSkillRepository.save(userSkill);

            LearningProgress progress = new LearningProgress();
            progress.setUserId(user.getId());
            progress.setRoleName(roleDefinition.name());
            progress.setSkillName(requirement.skillName());
            progress.setCurrentLevel(currentLevel);
            progress.setTargetLevel(requirement.targetLevel());
            progress.setEstimatedHours(estimateHours(currentLevel, requirement.targetLevel()));
            progress.setDisplayOrder(displayOrder++);
            progress.setStatus(missing ? ProgressStatus.NOT_STARTED : ProgressStatus.COMPLETED);
            progress.setCompletionPercent(missing ? Math.max(0, levelScore(currentLevel) * 25) : 100);
            progress.setProficiencyScore(calculateProficiencyScore(currentLevel, requirement.targetLevel()));
            progress.setWeeklyGoalHours(weeklyHours);
            progress.setConfidenceScore(Math.max(20, calculateProficiencyScore(currentLevel, requirement.targetLevel())));
            progress.setReminderDate(LocalDate.now().plusDays(displayOrder * 3L));
            progress.setReminderMessage("Spend " + Math.min(progress.getEstimatedHours(), weeklyHours) + " hours on " + requirement.skillName() + " this week.");
            progress.setMilestoneName(missing ? milestoneFor(requirement.targetLevel()) : "Completed");
            learningProgressRepository.save(progress);

            SkillRequirementResponse response = new SkillRequirementResponse(
                    requirement.skillName(), requirement.targetLevel(), currentLevel, requirement.category(), requirement.description(), missing);
            requiredSkills.add(response);
            if (missing) {
                missingSkills.add(response);
            } else {
                strengths.add(response);
            }

            readinessPoints += Math.min(levelScore(currentLevel), levelScore(requirement.targetLevel()));
            totalPossiblePoints += levelScore(requirement.targetLevel());
        }

        int readinessScore = totalPossiblePoints == 0 ? 0 : (readinessPoints * 100) / totalPossiblePoints;
        int estimatedWeeks = Math.max(1, (int) Math.ceil((double) learningProgressRepository.findByUserIdOrderByDisplayOrderAsc(user.getId()).stream()
                .filter(item -> item.getStatus() != ProgressStatus.COMPLETED)
                .mapToInt(LearningProgress::getEstimatedHours)
                .sum() / weeklyHours));
        String recommendation = missingSkills.isEmpty()
                ? "You are role-ready."
                : "Focus on your weaker skills first.";

        issueRoleReadyBadgeIfEligible(user.getId());

        return new SkillAssessmentResponse(
                roleDefinition.name(),
                readinessScore,
                weeklyHours,
                estimatedWeeks,
                requiredSkills,
                missingSkills,
                strengths,
                List.of("Foundation checkpoint", "Applied practice checkpoint", "Interview readiness checkpoint"),
                recommendation);
    }

    public List<RoadmapItemResponse> getRoadmap(Long userId) {
        authService.getUser(userId);
        List<LearningProgress> pendingItems = learningProgressRepository.findByUserIdOrderByDisplayOrderAsc(userId).stream()
                .filter(progress -> progress.getStatus() != ProgressStatus.COMPLETED || progress.getCompletionPercent() < 100)
                .toList();

        List<RoadmapItemResponse> weeklyRoadmap = new ArrayList<>();
        int weekNumber = 1;
        int plannedHoursThisWeek = 0;

        for (LearningProgress progress : pendingItems) {
            int weeklyCapacity = progress.getWeeklyGoalHours() == null ? 8 : progress.getWeeklyGoalHours();
            if (!weeklyRoadmap.isEmpty() && plannedHoursThisWeek + progress.getEstimatedHours() > weeklyCapacity) {
                weekNumber++;
                plannedHoursThisWeek = 0;
            }
            plannedHoursThisWeek += progress.getEstimatedHours();
            weeklyRoadmap.add(toRoadmapItem(progress, weekNumber));
        }

        return weeklyRoadmap;
    }

    @Transactional
    public RoadmapItemResponse updateProgress(ProgressUpdateRequest request) {
        LearningProgress progress = learningProgressRepository.findByUserIdAndSkillNameIgnoreCase(request.userId(), request.skillName())
                .orElseThrow(() -> new EntityNotFoundException("Roadmap item not found."));

        progress.setStatus(request.status());
        progress.setCompletionPercent(request.status() == ProgressStatus.COMPLETED ? 100 : request.completionPercent());
        learningProgressRepository.save(progress);
        issueRoleReadyBadgeIfEligible(request.userId());
        return toRoadmapItem(progress);
    }

    @Transactional
    public RoadmapItemResponse updateRoadmapPreferences(RoadmapPreferenceUpdateRequest request) {
        LearningProgress progress = learningProgressRepository.findByUserIdAndSkillNameIgnoreCase(request.userId(), request.skillName())
                .orElseThrow(() -> new EntityNotFoundException("Roadmap item not found."));
        if (request.notes() != null) {
            progress.setNotes(request.notes());
        }
        if (request.bookmarked() != null) {
            progress.setBookmarked(request.bookmarked());
        }
        if (request.confidenceScore() != null) {
            progress.setConfidenceScore(request.confidenceScore());
        }
        if (request.reminderDate() != null) {
            progress.setReminderDate(request.reminderDate());
            progress.setReminderMessage("Reminder: revisit " + progress.getSkillName() + " and complete the weekly milestone.");
        }
        learningProgressRepository.save(progress);
        return toRoadmapItem(progress);
    }

    public DashboardResponse getDashboard(Long userId) {
        var user = authService.getUser(userId);
        List<LearningProgress> items = learningProgressRepository.findByUserIdOrderByDisplayOrderAsc(userId);

        int completed = (int) items.stream().filter(item -> item.getStatus() == ProgressStatus.COMPLETED).count();
        int inProgress = (int) items.stream().filter(item -> item.getStatus() == ProgressStatus.IN_PROGRESS).count();
        int pending = (int) items.stream().filter(item -> item.getStatus() == ProgressStatus.NOT_STARTED).count();
        int total = items.size();
        int readiness = total == 0 ? 0 : items.stream().mapToInt(LearningProgress::getCompletionPercent).sum() / total;
        int completionRate = total == 0 ? 0 : (completed * 100) / total;
        int hoursLeft = items.stream().filter(item -> item.getStatus() != ProgressStatus.COMPLETED).mapToInt(LearningProgress::getEstimatedHours).sum();

        List<RoadmapItemResponse> spotlight = items.stream()
                .filter(item -> item.getStatus() != ProgressStatus.COMPLETED)
                .sorted(Comparator.comparingInt(LearningProgress::getCompletionPercent))
                .limit(3)
                .map(progress -> toRoadmapItem(progress, 0))
                .toList();

        List<QuizAttemptSummary> recentResults = quizAttemptRepository.findTop5ByUserIdOrderByCompletedAtDesc(userId).stream()
                .map(attempt -> new QuizAttemptSummary(
                        attempt.getSkillName(),
                        attempt.getPercentage(),
                        attempt.isPassed(),
                        attempt.getCompletedAt().toString(),
                        quizAttemptRepository.findByUserIdAndSkillNameIgnoreCaseOrderByCompletedAtDesc(userId, attempt.getSkillName()).size()))
                .toList();

        List<AchievementResponse> achievements = achievementRepository.findTop8ByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(item -> new AchievementResponse(item.getTitle(), item.getCategory(), item.getDescription(), item.getIssuedAt().toString()))
                .toList();

        int streakDays = calculateStreakDays(quizAttemptRepository.findTop5ByUserIdOrderByCompletedAtDesc(userId));
        ProgressAnalyticsResponse analytics = buildAnalytics(userId, items);
        List<WeakAreaInsightResponse> weakAreaInsights = buildWeakAreaInsights(items.stream()
                .filter(item -> item.getStatus() != ProgressStatus.COMPLETED)
                .sorted(Comparator.comparingInt(LearningProgress::getCompletionPercent))
                .limit(4)
                .toList());
        List<JobMatchResponse> jobMatches = externalJobMatchService.matchJobs(
                user.getRole() == null ? "" : user.getRole(),
                items.stream().filter(item -> item.getStatus() == ProgressStatus.COMPLETED).map(LearningProgress::getSkillName).collect(java.util.stream.Collectors.toSet()),
                items.stream().filter(item -> item.getStatus() != ProgressStatus.COMPLETED).map(LearningProgress::getSkillName).collect(java.util.stream.Collectors.toSet()));

        return new DashboardResponse(
                user.getUsername(),
                user.getRole(),
                readiness,
                completionRate,
                streakDays,
                completed,
                inProgress,
                pending,
                total,
                hoursLeft,
                spotlight,
                recentResults,
                achievements,
                analytics,
                jobMatches,
                weakAreaInsights);
    }

    public QuizPayloadResponse getQuiz(Long userId, String skillName) {
        authService.getUser(userId);
        LearningProgress progress = learningProgressRepository.findByUserIdAndSkillNameIgnoreCase(userId, skillName)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Roadmap item not found for quiz."));
        List<CatalogService.QuizQuestion> questions = catalogService.getQuizQuestions(skillName, progress.getCurrentLevel(), progress.getTargetLevel());
        if (questions.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "No quiz is available for this skill yet.");
        }
        String recommendedDifficulty = progress.getTargetLevel() == SkillLevel.ADVANCED ? "Advanced" : "Intermediate";
        return new QuizPayloadResponse(
                skillName,
                recommendedDifficulty,
                progress.getConfidenceScore() == null ? 0 : progress.getConfidenceScore(),
                questions.stream().map(question -> new QuizQuestionResponse(
                        question.id(),
                        question.prompt(),
                        question.options(),
                        question.questionType(),
                        question.difficulty(),
                        question.concept(),
                        question.evaluationCriteria())).toList());
    }

    @Transactional
    public QuizResultResponse submitQuiz(QuizSubmissionRequest request) {
        authService.getUser(request.userId());
        LearningProgress progress = learningProgressRepository.findByUserIdAndSkillNameIgnoreCase(request.userId(), request.skillName())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Roadmap item not found for quiz."));
        List<CatalogService.QuizQuestion> questions = catalogService.getQuizQuestions(request.skillName(), progress.getCurrentLevel(), progress.getTargetLevel());
        if (questions.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "No quiz is available for this skill yet.");
        }

        Map<String, String> answers = new HashMap<>();
        if (request.answers() != null) {
            request.answers().forEach(answer -> answers.put(answer.questionId(),
                    answer.selectedOption() != null && !answer.selectedOption().isBlank() ? answer.selectedOption() : answer.responseText()));
        }

        int score = 0;
        List<String> feedback = new ArrayList<>();
        List<String> weakAreas = new ArrayList<>();
        List<String> nextActions = new ArrayList<>();
        for (CatalogService.QuizQuestion question : questions) {
            String submittedAnswer = answers.get(question.id());
            boolean correct = question.answer() == null
                    ? submittedAnswer != null && submittedAnswer.trim().length() > 20
                    : question.answer().equals(submittedAnswer);
            if (correct) {
                score++;
                feedback.add("Strong: " + question.prompt());
            } else {
                feedback.add("Review: " + question.prompt() + " - " + question.explanation());
                weakAreas.add(question.concept());
                nextActions.add("Revise " + question.concept() + " for " + request.skillName() + ".");
            }
        }

        int percentage = (score * 100) / questions.size();
        boolean passed = percentage >= 70;
        List<QuizAttempt> previousAttempts = quizAttemptRepository.findByUserIdAndSkillNameIgnoreCaseOrderByCompletedAtDesc(request.userId(), request.skillName());
        int previousBest = previousAttempts.stream().mapToInt(QuizAttempt::getPercentage).max().orElse(0);
        int improvementDelta = percentage - previousBest;

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(request.userId());
        attempt.setSkillName(request.skillName());
        attempt.setScore(score);
        attempt.setTotalQuestions(questions.size());
        attempt.setPercentage(percentage);
        attempt.setPassed(passed);
        quizAttemptRepository.save(attempt);

        int measuredConfidence = Math.max(25, percentage);
        progress.setConfidenceScore((request.confidenceRating() == null ? measuredConfidence : (request.confidenceRating() + measuredConfidence) / 2));
        if (passed) {
            progress.setStatus(ProgressStatus.COMPLETED);
            progress.setCompletionPercent(100);
            progress.setMilestoneName("Job-ready checkpoint");
            issueAchievement(request.userId(), request.skillName() + " Certificate", "Certificate", "Passed the " + request.skillName() + " quiz.");
        } else if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
            progress.setCompletionPercent(Math.max(progress.getCompletionPercent(), 65));
            progress.setReminderMessage("Revisit weak areas in " + request.skillName() + " and retake the quiz.");
        }
        learningProgressRepository.save(progress);
        rebalanceRoadmapPriorities(request.userId(), request.skillName(), weakAreas);

        issueRoleReadyBadgeIfEligible(request.userId());

        return new QuizResultResponse(
                request.skillName(),
                score,
                questions.size(),
                percentage,
                passed,
                request.confidenceRating() == null ? 0 : request.confidenceRating(),
                measuredConfidence,
                feedback,
                weakAreas.stream().distinct().toList(),
                nextActions.isEmpty() ? List.of("Proceed to the next roadmap item.") : nextActions.stream().distinct().toList(),
                buildWeakAreaInsights(List.of(progress)),
                previousAttempts.size() + 1,
                improvementDelta);
    }

    public MockInterviewPayloadResponse getMockInterview(Long userId, String skillName) {
        User user = authService.getUser(userId);
        return new MockInterviewPayloadResponse(user.getRole(), skillName, catalogService.getMockInterviewQuestions(user.getRole(), skillName));
    }

    public MockInterviewResultResponse evaluateMockInterview(MockInterviewSubmissionRequest request) {
        authService.getUser(request.userId());
        List<MockInterviewQuestionResponse> questions = catalogService.getMockInterviewQuestions(request.roleName(), request.skillName());
        Map<String, String> answers = new HashMap<>();
        if (request.answers() != null) {
            request.answers().forEach(answer -> answers.put(answer.questionId(), answer.answer()));
        }

        int score = 0;
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        for (MockInterviewQuestionResponse question : questions) {
            String answer = answers.get(question.id());
            if (answer != null && answer.trim().length() >= 40) {
                score += 50;
                strengths.add("Strong response for " + question.focusArea());
            } else {
                improvements.add("Expand your answer for " + question.focusArea() + " with clearer examples and outcomes.");
            }
        }
        return new MockInterviewResultResponse(
                request.roleName(),
                request.skillName(),
                Math.min(score, 100),
                strengths.isEmpty() ? List.of("You attempted the interview flow.") : strengths,
                improvements.isEmpty() ? List.of("Keep practicing concise and structured answers.") : improvements,
                List.of("Use the STAR method", "Mention real projects", "Tie answers back to business impact"));
    }

    public RoadmapItemResponse toRoadmapItem(LearningProgress progress) {
        return toRoadmapItem(progress, 0);
    }

    public RoadmapItemResponse toRoadmapItem(LearningProgress progress, int weekNumber) {
        var roleDefinition = catalogService.findRole(progress.getRoleName()).orElse(null);
        CatalogService.SkillRequirement requirement = roleDefinition == null ? null : roleDefinition.skills().stream()
                .filter(skill -> skill.skillName().equalsIgnoreCase(progress.getSkillName()))
                .findFirst()
                .orElse(null);

        String weekLabel = weekNumber > 0 ? "Week " + weekNumber : "Next Focus";
        String weeklyGoal = weekNumber > 0
                ? "Study " + progress.getSkillName() + " and reach " + progress.getTargetLevel() + " level this week."
                : "Keep improving " + progress.getSkillName() + " to move closer to your target role.";

        return new RoadmapItemResponse(
                progress.getSkillName(),
                progress.getCurrentLevel(),
                progress.getTargetLevel(),
                progress.getStatus(),
                progress.getCompletionPercent(),
                progress.getEstimatedHours(),
                progress.getProficiencyScore() == null ? calculateProficiencyScore(progress.getCurrentLevel(), progress.getTargetLevel()) : progress.getProficiencyScore(),
                weekNumber,
                weekLabel,
                weeklyGoal,
                progress.getReminderMessage(),
                progress.getReminderDate() == null ? null : progress.getReminderDate().toString(),
                progress.getConfidenceScore(),
                progress.getNotes(),
                Boolean.TRUE.equals(progress.isBookmarked()),
                requirement != null ? requirement.category() : "Learning",
                requirement != null ? requirement.description() : "Improve this skill to move closer to your goal.",
                catalogService.getPrerequisites(progress.getSkillName()),
                catalogService.getMiniProject(progress.getSkillName()),
                catalogService.getPortfolioProject(progress.getSkillName()),
                catalogService.getGuidedLearningPath(progress.getSkillName(), progress.getCurrentLevel(), progress.getTargetLevel()),
                catalogService.getResources(progress.getSkillName()),
                buildDailyPlan(progress));
    }

    private int levelScore(SkillLevel level) {
        return switch (level) {
            case BEGINNER -> 1;
            case INTERMEDIATE -> 2;
            case ADVANCED -> 3;
        };
    }

    private SkillLevel inferLevel(int skillScore, int totalQuestions, SkillLevel targetLevel) {
        if (totalQuestions == 0) {
            return SkillLevel.BEGINNER;
        }
        int percentage = (skillScore * 100) / totalQuestions;
        if (percentage >= 85) {
            return targetLevel;
        }
        if (percentage >= 60) {
            return targetLevel == SkillLevel.ADVANCED ? SkillLevel.INTERMEDIATE : SkillLevel.BEGINNER;
        }
        return SkillLevel.BEGINNER;
    }

    private int estimateHours(SkillLevel currentLevel, SkillLevel targetLevel) {
        int gap = Math.max(0, levelScore(targetLevel) - levelScore(currentLevel));
        return switch (gap) {
            case 0 -> 2;
            case 1 -> 12;
            default -> 24;
        };
    }

    private int calculateProficiencyScore(SkillLevel currentLevel, SkillLevel targetLevel) {
        return Math.min(100, (levelScore(currentLevel) * 100) / Math.max(1, levelScore(targetLevel)));
    }

    private String milestoneFor(SkillLevel targetLevel) {
        return switch (targetLevel) {
            case BEGINNER -> "Foundation checkpoint";
            case INTERMEDIATE -> "Applied practice checkpoint";
            case ADVANCED -> "Interview readiness checkpoint";
        };
    }

    private void issueAchievement(Long userId, String title, String category, String description) {
        if (achievementRepository.existsByUserIdAndTitleIgnoreCase(userId, title)) {
            return;
        }
        Achievement achievement = new Achievement();
        achievement.setUserId(userId);
        achievement.setTitle(title);
        achievement.setCategory(category);
        achievement.setDescription(description);
        achievementRepository.save(achievement);
    }

    private void issueRoleReadyBadgeIfEligible(Long userId) {
        List<LearningProgress> progressItems = learningProgressRepository.findByUserIdOrderByDisplayOrderAsc(userId);
        boolean allDone = !progressItems.isEmpty() && progressItems.stream().allMatch(item -> item.getStatus() == ProgressStatus.COMPLETED);
        if (allDone) {
            User user = authService.getUser(userId);
            issueAchievement(userId, user.getRole() + " Readiness Badge", "Badge", "Completed all tracked roadmap skills for " + user.getRole() + ".");
        }
    }

    public ResumeAnalysisResponse analyzeResume(ResumeAnalysisRequest request) {
        authService.getUser(request.userId());
        var roleDefinition = catalogService.findRole(request.role())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Selected role was not found."));

        String resume = request.resumeText().toLowerCase();
        List<String> detectedSkills = roleDefinition.skills().stream()
                .map(CatalogService.SkillRequirement::skillName)
                .filter(skill -> resume.contains(skill.toLowerCase()))
                .toList();
        List<String> missingSkills = roleDefinition.skills().stream()
                .map(CatalogService.SkillRequirement::skillName)
                .filter(skill -> !resume.contains(skill.toLowerCase()))
                .toList();

        return new ResumeAnalysisResponse(
                roleDefinition.name(),
                detectedSkills,
                missingSkills,
                missingSkills.isEmpty()
                        ? "Your resume already reflects the core skills for this role."
                        : "Your resume is missing visible evidence for " + String.join(", ", missingSkills.subList(0, Math.min(3, missingSkills.size()))) + ". Add proof through projects, experience, or certifications.");
    }

    public RoleComparisonResponse compareRoles(String primaryRole, String compareRole) {
        var first = catalogService.findRole(primaryRole).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Role not found."));
        var second = catalogService.findRole(compareRole).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Role not found."));
        List<String> firstSkills = first.skills().stream().map(CatalogService.SkillRequirement::skillName).distinct().toList();
        List<String> secondSkills = second.skills().stream().map(CatalogService.SkillRequirement::skillName).distinct().toList();
        List<String> shared = firstSkills.stream().filter(secondSkills::contains).toList();
        List<String> firstOnly = firstSkills.stream().filter(skill -> !secondSkills.contains(skill)).toList();
        List<String> secondOnly = secondSkills.stream().filter(skill -> !firstSkills.contains(skill)).toList();
        return new RoleComparisonResponse(first.name(), second.name(), shared, firstOnly, secondOnly);
    }

    private List<DailyStudyTaskResponse> buildDailyPlan(LearningProgress progress) {
        int totalHours = Math.max(4, progress.getEstimatedHours());
        return List.of(
                new DailyStudyTaskResponse(1, "Learn", "Study foundations of " + progress.getSkillName(), Math.max(1, totalHours / 4) + "h"),
                new DailyStudyTaskResponse(2, "Practice", "Solve guided exercises for " + progress.getSkillName(), Math.max(1, totalHours / 4) + "h"),
                new DailyStudyTaskResponse(3, "Apply", "Work on a mini task or use case", Math.max(1, totalHours / 4) + "h"),
                new DailyStudyTaskResponse(4, "Review", "Revise weak concepts and take a checkpoint quiz", Math.max(1, totalHours / 4) + "h"));
    }

    private void rebalanceRoadmapPriorities(Long userId, String focusSkill, List<String> weakAreas) {
        List<LearningProgress> items = new ArrayList<>(learningProgressRepository.findByUserIdOrderByDisplayOrderAsc(userId));
        items.sort((left, right) -> {
            boolean leftPriority = left.getSkillName().equalsIgnoreCase(focusSkill) || weakAreas.stream().anyMatch(area -> area.equalsIgnoreCase(left.getSkillName()));
            boolean rightPriority = right.getSkillName().equalsIgnoreCase(focusSkill) || weakAreas.stream().anyMatch(area -> area.equalsIgnoreCase(right.getSkillName()));
            if (leftPriority == rightPriority) {
                return Integer.compare(left.getCompletionPercent(), right.getCompletionPercent());
            }
            return leftPriority ? -1 : 1;
        });
        for (int index = 0; index < items.size(); index++) {
            items.get(index).setDisplayOrder(index + 1);
            learningProgressRepository.save(items.get(index));
        }
    }

    private ProgressAnalyticsResponse buildAnalytics(Long userId, List<LearningProgress> items) {
        List<QuizAttempt> attempts = quizAttemptRepository.findTop5ByUserIdOrderByCompletedAtDesc(userId);
        int average = attempts.isEmpty() ? 0 : (int) attempts.stream().mapToInt(QuizAttempt::getPercentage).average().orElse(0);
        int latest = attempts.isEmpty() ? 0 : attempts.get(0).getPercentage();
        int previous = attempts.size() > 1 ? attempts.get(1).getPercentage() : latest;
        List<String> weakest = items.stream()
                .filter(item -> item.getStatus() != ProgressStatus.COMPLETED)
                .sorted(Comparator.comparingInt(LearningProgress::getCompletionPercent))
                .limit(4)
                .map(LearningProgress::getSkillName)
                .toList();
        return new ProgressAnalyticsResponse(average, latest, latest - previous, attempts.size(), weakest);
    }

    private List<WeakAreaInsightResponse> buildWeakAreaInsights(List<LearningProgress> items) {
        return items.stream()
                .map(item -> new WeakAreaInsightResponse(
                        item.getSkillName(),
                        item.getSkillName() + " is still below your target level, which means it can reduce your readiness for the role.",
                        "Start with " + catalogService.getPrerequisites(item.getSkillName()).get(0) + " and then follow the first guided learning step."))
                .toList();
    }

    private int calculateStreakDays(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) {
            return 0;
        }
        List<LocalDate> dates = attempts.stream()
                .map(attempt -> attempt.getCompletedAt().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        int streak = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i - 1).minusDays(1).equals(dates.get(i))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}
