package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.data.ExerciseStageData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.data.AttemptExerciseData;
import org.vstu.compprehension.models.data.AttemptInteractionData;
import org.vstu.compprehension.models.data.AttemptQuestionData;
import org.vstu.compprehension.models.data.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.models.data.QuestionMetadataBitsData;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository.AttemptOwner;
import org.vstu.compprehension.models.repository.InteractionRepository;
import org.vstu.compprehension.models.repository.InteractionRepository.InteractionLawRow;
import org.vstu.compprehension.models.repository.InteractionRepository.InteractionRow;
import org.vstu.compprehension.models.repository.QuestionRepository;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExerciseAttemptService {
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseService exerciseService;
    private final UserRepository userRepository;
    private final LtiContextProvider ltiContextProvider;
    private final GradePassbackService gradePassbackService;
    private final CourseService courseService;
    private final AuthService authService;
    private final AuthScopeFactory authScopes;
    private final QuestionRepository questionRepository;
    private final InteractionRepository interactionRepository;

    public ExerciseAttemptService(ExerciseAttemptRepository exerciseAttemptRepository,
                                  ExerciseService exerciseService,
                                  UserRepository userRepository,
                                  LtiContextProvider ltiContextProvider,
                                  GradePassbackService gradePassbackService,
                                  CourseService courseService,
                                  AuthService authService,
                                  AuthScopeFactory authScopes,
                                  QuestionRepository questionRepository,
                                  InteractionRepository interactionRepository) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.exerciseService = exerciseService;
        this.userRepository = userRepository;
        this.ltiContextProvider = ltiContextProvider;
        this.gradePassbackService = gradePassbackService;
        this.courseService = courseService;
        this.authService = authService;
        this.authScopes = authScopes;
        this.questionRepository = questionRepository;
        this.interactionRepository = interactionRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ExerciseAttemptEntity> findById(Long attemptId) {
        return exerciseAttemptRepository.findById(attemptId);
    }

    /**
     * Попытка со всеми вопросами и взаимодействиями — в виде отсоединённых данных.
     * <p>
     * Ровно пять запросов независимо от размера попытки: попытка с упражнением и доменом,
     * вопросы с метаданными, взаимодействия, нарушенные законы, верно применённые законы.
     * Обхода ленивого графа нет, поэтому потребителю (стратегии) не нужны ни сессия
     * Hibernate, ни знание о том, как это разложено по таблицам.
     */
    @Transactional(readOnly = true)
    public @NotNull ExerciseAttemptWithQuestionsData getAttemptWithQuestions(long attemptId) {
        var attempt = exerciseAttemptRepository.findByIdFetchingExerciseAndDomain(attemptId)
                .orElseThrow(() -> new NoSuchElementException("Exercise attempt " + attemptId + " not found"));
        var exercise = attempt.getExercise();
        var exerciseData = new AttemptExerciseData(
                exercise.getId(),
                exercise.getDomain().getName(),
                exercise.getStages() == null ? List.of() : List.copyOf(exercise.getStages()),
                exercise.getTags());

        var questions = questionRepository.findAllByAttemptIdFetchingMetadata(attemptId);
        var questionIds = questions.stream().map(QuestionEntity::getId).toList();

        var interactionRows = questionIds.isEmpty()
                ? List.<InteractionRow>of()
                : interactionRepository.findRowsByQuestionIdIn(questionIds);
        var interactionIds = interactionRows.stream().map(InteractionRow::interactionId).toList();

        Map<Long, List<String>> violationsByInteraction = interactionIds.isEmpty()
                ? Map.of() : groupLawNames(interactionRepository.findViolationLawsByInteractionIdIn(interactionIds));
        Map<Long, List<String>> correctLawsByInteraction = interactionIds.isEmpty()
                ? Map.of() : groupLawNames(interactionRepository.findCorrectLawsByInteractionIdIn(interactionIds));

        Map<Long, List<AttemptInteractionData>> interactionsByQuestion = interactionRows.stream()
                .collect(Collectors.groupingBy(
                        InteractionRow::questionId,
                        Collectors.mapping(row -> new AttemptInteractionData(
                                row.interactionId(),
                                row.orderNumber() == null ? 0 : row.orderNumber(),
                                row.interactionType(),
                                row.interactionsLeft(),
                                violationsByInteraction.getOrDefault(row.interactionId(), List.of()),
                                correctLawsByInteraction.getOrDefault(row.interactionId(), List.of())
                        ), Collectors.toList())));

        var questionsData = questions.stream()
                .map(q -> new AttemptQuestionData(
                        q.getId(),
                        q.getQuestionName(),
                        q.getQuestionDomainType(),
                        toBits(q.getMetadata()),
                        interactionsByQuestion.getOrDefault(q.getId(), List.of())))
                .toList();

        // getUser() ленивый, но getId() обслуживается самим прокси и запроса не делает
        return new ExerciseAttemptWithQuestionsData(
                attempt.getId(), attempt.getUser().getId(), exerciseData, questionsData);
    }

    private static Map<Long, List<String>> groupLawNames(List<InteractionLawRow> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                InteractionLawRow::interactionId,
                Collectors.mapping(InteractionLawRow::lawName, Collectors.toList())));
    }

    private static @Nullable QuestionMetadataBitsData toBits(@Nullable QuestionMetadataEntity metadata) {
        if (metadata == null) {
            return null;
        }
        // Формулы остаются в сущности, здесь только снятый результат.
        return new QuestionMetadataBitsData(
                metadata.getId(),
                metadata.traceConceptsSatisfiedFromPlan(),
                metadata.traceConceptsUnsatisfiedFromPlan(),
                metadata.traceConceptsSatisfiedFromRequest(),
                metadata.getConceptBitsInRequest(),
                metadata.violationsSatisfiedFromPlan(),
                metadata.violationsUnsatisfiedFromPlan(),
                metadata.violationsSatisfiedFromRequest(),
                metadata.getViolationBitsInRequest(),
                metadata.getSkillBits());
    }

    /**
     * Этап упражнения, на котором задан вопрос.
     * <p>
     * Раньше это считал сам вопрос, обходя {@code getExerciseAttempt().getQuestions()}
     * и поднимая ради одного этапа все вопросы попытки. Здесь — попытка с упражнением
     * и один скалярный запрос за порядковым номером.
     *
     * @return пусто, если вопрос не привязан к попытке или у упражнения нет этапов
     */
    @Transactional(readOnly = true)
    public Optional<ExerciseStageData> findStageForQuestion(long questionId) {
        var attempt = exerciseAttemptRepository.findByQuestionId(questionId).orElse(null);
        if (attempt == null) {
            return Optional.empty();
        }
        var stages = attempt.getExercise().getStages();
        if (stages == null || stages.isEmpty()) {
            return Optional.empty();
        }

        long questionNumber = questionRepository.countUpToQuestionInAttempt(attempt.getId(), questionId);
        int questionsPassed = 0;
        ExerciseStageData stage = stages.getFirst();
        for (int i = 0; i < stages.size() && questionsPassed < questionNumber; i++) {
            stage = stages.get(i);
            questionsPassed += stage.getNumberOfQuestions();
        }
        return Optional.ofNullable(stage);
    }

    /**
     * Язык, выбранный автором попытки, породившей вопрос.
     *
     * @return {@code RUSSIAN}, если вопрос не привязан к попытке — как было и раньше
     */
    @Transactional(readOnly = true)
    public Language findUserLanguageForQuestion(long questionId) {
        return exerciseAttemptRepository.findByQuestionId(questionId)
                .map(attempt -> attempt.getUser().getPreferred_language())
                .orElse(Language.RUSSIAN);
    }

    /** Идентификатор попытки, в рамках которой задан вопрос. */
    @Transactional(readOnly = true)
    public Optional<Long> findAttemptIdOfQuestion(long questionId) {
        return exerciseAttemptRepository.findByQuestionId(questionId).map(ExerciseAttemptEntity::getId);
    }

    /**
     * Включён ли для упражнения этого вопроса режим вспомогательных вопросов
     * на дереве решений. Для вопроса вне попытки — да, как было и раньше.
     */
    @Transactional(readOnly = true)
    public boolean prefersDecisionTreeSupplementary(long questionId) {
        return exerciseAttemptRepository.findByQuestionId(questionId)
                .map(attempt -> attempt.getExercise().getOptions()
                        .isPreferDecisionTreeBasedSupplementaryEnabled())
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public void ensureCanAccessAttempt(long userId, long attemptId) {
        AttemptOwner owner = exerciseAttemptRepository.findOwnerByAttemptId(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("No attempt with id " + attemptId));
        ensureOwnerOrPrivileged(userId, owner, attemptId);
    }

    @Transactional(readOnly = true)
    public void ensureCanAccessQuestion(long userId, long questionId) {
        AttemptOwner owner = exerciseAttemptRepository.findOwnerByQuestionId(questionId)
                .orElse(null);
        ensureOwnerOrPrivileged(userId, owner, questionId);
    }

    private void ensureOwnerOrPrivileged(long userId, @Nullable AttemptOwner owner, long targetId) {
        if (owner != null && owner.userId() != null && owner.userId() == userId) {
            authService.ensureAuthorized(userId, SystemPermission.SOLVE_EXERCISE, authScopes.courseOrGlobal(owner.courseId()));
            return;
        }
        if (authService.isAuthorized(userId, SystemPermission.EDIT_EXERCISE, authScopes.courseOrGlobal(owner != null ? owner.courseId() : null))) {
            return;
        }
        throw new SecurityException(String.format(
                "User %s is not allowed to access attempt data %s", userId, targetId));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ExerciseAttemptEntity createNewAttempt(@NotNull Long exerciseId, @NotNull Long userId, Long courseId) {
        CourseEntity course = null;
        if (courseId != null) {
            ExerciseCourseLinkEntity exerciseCourse = courseService.findExerciseCourseLinkOrThrow(exerciseId, courseId);
            course = exerciseCourse.getCourse();
            exerciseAttemptRepository.changeExistingAttemptsStatusByCourse(
                    exerciseId, courseId, userId, AttemptStatus.INCOMPLETE, AttemptStatus.COMPLETED_BY_SYSTEM);
        } else {
            exerciseAttemptRepository.changeExistingAttemptsStatus(
                    exerciseId, userId, AttemptStatus.INCOMPLETE, AttemptStatus.COMPLETED_BY_SYSTEM);
        }

        var exercise = exerciseService.getExercise(exerciseId);
        var user = userRepository.findById(userId).orElseThrow();

        var ea = new ExerciseAttemptEntity();
        ea.setExercise(exercise);
        ea.setCourse(course);
        ea.setUser(user);
        ea.setAttemptStatus(AttemptStatus.INCOMPLETE);
        ea.setQuestions(new ArrayList<>());

        ltiContextProvider.getCurrentLtiContext().ifPresent(ctx -> {
            ea.setLtiLineitemUrl(ctx.lineitemUrl());
            if (ctx.course() != null) {
                ea.setLtiContextId(ctx.course().courseId());
            }
        });

        exerciseAttemptRepository.save(ea);
        return ea;
    }

    public void ensureAttemptStatus(ExerciseAttemptEntity attempt, Decision decision) {
        if (decision == Decision.FINISH && attempt.getAttemptStatus() == AttemptStatus.INCOMPLETE) {
            attempt.setAttemptStatus(AttemptStatus.COMPLETED_BY_USER);
            exerciseAttemptRepository.save(attempt);
            double grade = exerciseAttemptRepository.calculateFinalGrade(attempt.getId())
                    .orElse(0.0);
            gradePassbackService.passGrade(attempt, grade);
        } else {
            exerciseAttemptRepository.save(attempt);
        }
    }
}
