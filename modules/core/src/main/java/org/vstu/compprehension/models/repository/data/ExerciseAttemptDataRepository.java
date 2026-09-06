package org.vstu.compprehension.models.repository.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.data.AttemptExerciseData;
import org.vstu.compprehension.models.data.AttemptInteractionData;
import org.vstu.compprehension.models.data.AttemptOwnerData;
import org.vstu.compprehension.models.data.AttemptQuestionData;
import org.vstu.compprehension.models.data.AttemptSummaryData;
import org.vstu.compprehension.models.data.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.models.data.GradePassbackTargetData;
import org.vstu.compprehension.models.data.QuestionAttemptContextData;
import org.vstu.compprehension.models.data.QuestionMetadataBitsData;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository.AttemptOwner;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository.AttemptSummaryRow;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository.GradePassbackTargetRow;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.InteractionRepository;
import org.vstu.compprehension.models.repository.InteractionRepository.InteractionLawRow;
import org.vstu.compprehension.models.repository.InteractionRepository.InteractionRow;
import org.vstu.compprehension.models.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Попытки прохождения упражнений в виде отсоединённых данных.
 * <p>
 * Читает только то, что нужно вызывающему: раньше все четыре вопроса «что известно
 * о попытке по этому вопросу» отвечались одним и тем же запросом, тянувшим вместе
 * с попыткой все её вопросы, — ради одного поля.
 */
@Repository
@RequiredArgsConstructor
public class ExerciseAttemptDataRepository {

    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final QuestionRepository questionRepository;
    private final InteractionRepository interactionRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    /**
     * Попытка со всеми вопросами и взаимодействиями.
     * <p>
     * Ровно пять запросов независимо от размера попытки: попытка с упражнением и доменом,
     * вопросы с метаданными, взаимодействия, нарушенные законы, верно применённые законы.
     * Обхода ленивого графа нет, поэтому потребителю (стратегии) не нужны ни сессия
     * Hibernate, ни знание о том, как это разложено по таблицам.
     *
     * @throws NoSuchElementException если попытки нет
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
        var interactionIds = interactionRows.stream().map(InteractionRow::getInteractionId).toList();

        Map<Long, List<String>> violationsByInteraction = interactionIds.isEmpty()
                ? Map.of() : groupLawNames(interactionRepository.findViolationLawsByInteractionIdIn(interactionIds));
        Map<Long, List<String>> correctLawsByInteraction = interactionIds.isEmpty()
                ? Map.of() : groupLawNames(interactionRepository.findCorrectLawsByInteractionIdIn(interactionIds));

        Map<Long, List<AttemptInteractionData>> interactionsByQuestion = interactionRows.stream()
                .collect(Collectors.groupingBy(
                        InteractionRow::getQuestionId,
                        Collectors.mapping(row -> new AttemptInteractionData(
                                row.getInteractionId(),
                                row.getOrderNumber() == null ? 0 : row.getOrderNumber(),
                                row.getInteractionType(),
                                row.getInteractionsLeft(),
                                violationsByInteraction.getOrDefault(row.getInteractionId(), List.of()),
                                correctLawsByInteraction.getOrDefault(row.getInteractionId(), List.of())
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

    /**
     * Контекст попытки, в которой задан вопрос: один запрос без подъёма её вопросов.
     *
     * @return пусто, если вопрос не привязан к попытке
     */
    @Transactional(readOnly = true)
    public @NotNull Optional<QuestionAttemptContextData> findQuestionAttemptContext(long questionId) {
        return exerciseAttemptRepository.findByQuestionIdFetchingExerciseAndUser(questionId)
                .map(ExerciseAttemptDataRepository::toContext);
    }

    /**
     * Порядковый номер вопроса внутри попытки, считая с единицы.
     * <p>
     * Раньше это считалось как {@code attempt.getQuestions().indexOf(question) + 1},
     * то есть ради одного числа поднимались все вопросы попытки.
     */
    @Transactional(readOnly = true)
    public long countQuestionsUpTo(long attemptId, long questionId) {
        return questionRepository.countUpToQuestionInAttempt(attemptId, questionId);
    }

    /** Владелец попытки; пусто, если попытки нет. */
    @Transactional(readOnly = true)
    public @NotNull Optional<AttemptOwnerData> findOwnerByAttemptId(long attemptId) {
        return exerciseAttemptRepository.findOwnerByAttemptId(attemptId)
                .map(ExerciseAttemptDataRepository::toOwner);
    }

    /** Владелец попытки, в которой задан вопрос; пусто, если вопрос вне попытки. */
    @Transactional(readOnly = true)
    public @NotNull Optional<AttemptOwnerData> findOwnerByQuestionId(long questionId) {
        return exerciseAttemptRepository.findOwnerByQuestionId(questionId)
                .map(ExerciseAttemptDataRepository::toOwner);
    }

    /** Попытка в объёме, который уезжает на фронт; пусто, если попытки нет. */
    @Transactional(readOnly = true)
    public @NotNull Optional<AttemptSummaryData> findSummary(long attemptId) {
        return exerciseAttemptRepository.findSummaryRow(attemptId).map(this::toSummary);
    }

    /**
     * Попытка пользователя по упражнению в заданном статусе.
     *
     * @param courseId если задан, ищется попытка именно в этом курсе; иначе курс не учитывается
     */
    @Transactional(readOnly = true)
    public @NotNull Optional<AttemptSummaryData> findSummaryWithStatus(
            long exerciseId, long userId, @Nullable Long courseId, @NotNull AttemptStatus status) {
        var row = courseId != null
                ? exerciseAttemptRepository.findSummaryRowWithStatusByCourse(exerciseId, courseId, userId, status)
                : exerciseAttemptRepository.findSummaryRowWithStatus(exerciseId, userId, status);
        return row.map(this::toSummary);
    }

    /**
     * Завести попытку, закрыв незавершённые попытки того же пользователя по этому упражнению.
     * <p>
     * Порядок сохранён: сначала массовое закрытие, потом вставка — иначе новая попытка
     * закрыла бы саму себя.
     *
     * @param courseId курс, в рамках которого идёт попытка; null — попытка вне курса
     */
    @Transactional
    public @NotNull AttemptSummaryData create(long exerciseId, long userId, @Nullable Long courseId,
                                              @Nullable String ltiLineitemUrl, @Nullable String ltiContextId) {
        if (courseId != null) {
            exerciseAttemptRepository.changeExistingAttemptsStatusByCourse(
                    exerciseId, courseId, userId, AttemptStatus.INCOMPLETE, AttemptStatus.COMPLETED_BY_SYSTEM);
        } else {
            exerciseAttemptRepository.changeExistingAttemptsStatus(
                    exerciseId, userId, AttemptStatus.INCOMPLETE, AttemptStatus.COMPLETED_BY_SYSTEM);
        }

        var attempt = new ExerciseAttemptEntity();
        attempt.setExercise(exerciseRepository.findById(exerciseId).orElseThrow(() ->
                new NoSuchElementException("Exercise with id: " + exerciseId + " not Found")));
        attempt.setUser(userRepository.findById(userId).orElseThrow(() ->
                new NoSuchElementException("User with id: " + userId + " not Found")));
        attempt.setCourse(courseId == null ? null : courseRepository.findById(courseId).orElseThrow(() ->
                new NoSuchElementException("Course with id: " + courseId + " not Found")));
        attempt.setAttemptStatus(AttemptStatus.INCOMPLETE);
        attempt.setQuestions(new ArrayList<>());
        attempt.setLtiLineitemUrl(ltiLineitemUrl);
        attempt.setLtiContextId(ltiContextId);

        exerciseAttemptRepository.save(attempt);
        return new AttemptSummaryData(attempt.getId(), userId, exerciseId, courseId,
                AttemptStatus.INCOMPLETE, List.of());
    }

    /**
     * Отметить попытку завершённой пользователем.
     *
     * @return false, если попытка уже была завершена — тогда оценку выставлять не надо
     */
    @Transactional
    public boolean finishIfIncomplete(long attemptId) {
        var attempt = exerciseAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new NoSuchElementException("Exercise attempt " + attemptId + " not found"));
        if (attempt.getAttemptStatus() != AttemptStatus.INCOMPLETE) {
            return false;
        }
        attempt.setAttemptStatus(AttemptStatus.COMPLETED_BY_USER);
        exerciseAttemptRepository.save(attempt);
        return true;
    }

    /**
     * Адресат оценки за попытку: одним запросом, без обхода связей.
     *
     * @return пусто, если попытки нет
     */
    @Transactional(readOnly = true)
    public @NotNull Optional<GradePassbackTargetData> findGradePassbackTarget(long attemptId) {
        return exerciseAttemptRepository.findGradePassbackTargetRow(attemptId)
                .map(ExerciseAttemptDataRepository::toGradePassbackTarget);
    }

    /** Итоговая оценка попытки; 0, если оценивать нечего. */
    @Transactional(readOnly = true)
    public double getFinalGrade(long attemptId) {
        return exerciseAttemptRepository.calculateFinalGrade(attemptId).orElse(0.0);
    }

    // ---------------------------------------------------------------- маппинг

    private @NotNull AttemptSummaryData toSummary(@NotNull AttemptSummaryRow row) {
        return new AttemptSummaryData(
                row.getAttemptId(),
                row.getUserId(),
                row.getExerciseId(),
                row.getCourseId(),
                row.getStatus(),
                exerciseAttemptRepository.findNonSupplementaryQuestionIds(row.getAttemptId()));
    }


    private static @NotNull QuestionAttemptContextData toContext(@NotNull ExerciseAttemptEntity attempt) {
        var exercise = attempt.getExercise();
        return new QuestionAttemptContextData(
                attempt.getId(),
                attempt.getUser().getPreferred_language(),
                exercise.getStages() == null ? List.of() : List.copyOf(exercise.getStages()),
                exercise.getOptions().isPreferDecisionTreeBasedSupplementaryEnabled());
    }

    private static @NotNull GradePassbackTargetData toGradePassbackTarget(
            @NotNull GradePassbackTargetRow row) {
        // Образовательный ресурс у курса обязателен схемой, поэтому вложенная запись
        // существует ровно тогда, когда есть сам курс.
        var course = row.getCourseId() == null ? null : new GradePassbackTargetData.CourseTarget(
                row.getCourseId(),
                row.getExternalCourseId(),
                new GradePassbackTargetData.EducationResourceTarget(
                        row.getEducationResourceId(),
                        row.getEducationResourceType(),
                        row.getEducationResourceUrl()));
        return new GradePassbackTargetData(
                row.getAttemptId(),
                row.getExerciseId(),
                row.getUserId(),
                row.getExternalUserId(),
                row.getLtiLineitemUrl(),
                course);
    }

    private static @NotNull AttemptOwnerData toOwner(@NotNull AttemptOwner owner) {
        return new AttemptOwnerData(owner.getUserId(), owner.getCourseId());
    }

    private static Map<Long, List<String>> groupLawNames(List<InteractionLawRow> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                InteractionLawRow::getInteractionId,
                Collectors.mapping(InteractionLawRow::getLawName, Collectors.toList())));
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
}
