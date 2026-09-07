package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.exerciseattempt.AttemptExerciseData;
import org.vstu.compprehension.data.exerciseattempt.AttemptGenerationContextData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptOwnerData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.data.exerciseattempt.GradePassbackTargetData;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;
import org.vstu.compprehension.data.question.QuestionMetadataBitsData;
import org.vstu.compprehension.enums.AttemptStatus;
import org.vstu.compprehension.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.AttemptOwner;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.AttemptSummaryRow;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.GradePassbackTargetRow;
import org.vstu.compprehension.repositories.entity.ExerciseRepository;
import org.vstu.compprehension.repositories.entity.UserRepository;
import org.vstu.compprehension.repositories.entity.CourseRepository;
import org.vstu.compprehension.repositories.entity.InteractionRepository;
import org.vstu.compprehension.repositories.entity.InteractionRepository.InteractionLawRow;
import org.vstu.compprehension.repositories.entity.InteractionRepository.InteractionRow;
import org.vstu.compprehension.repositories.entity.QuestionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExerciseAttemptDataRepository {
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final QuestionRepository questionRepository;
    private final InteractionRepository interactionRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

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

        Map<Long, List<AttemptQuestionInteractionData>> interactionsByQuestion = interactionRows.stream()
                .collect(Collectors.groupingBy(
                        InteractionRow::getQuestionId,
                        Collectors.mapping(row -> new AttemptQuestionInteractionData(
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

    @Transactional(readOnly = true)
    public @NotNull AttemptGenerationContextData getGenerationContext(long attemptId) {
        var attempt = exerciseAttemptRepository.findByIdFetchingExerciseDomainAndUser(attemptId)
                .orElseThrow(() -> new NoSuchElementException("Exercise attempt " + attemptId + " not found"));
        var exercise = attempt.getExercise();
        return new AttemptGenerationContextData(
                attempt.getId(),
                exercise.getDomain().getName(),
                exercise.getStrategyId(),
                exercise.getOptions(),
                attempt.getUser().getPreferred_language());
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<QuestionAttemptContextData> findQuestionAttemptContext(long questionId) {
        return exerciseAttemptRepository.findByQuestionIdFetchingExerciseAndUser(questionId)
                .map(ExerciseAttemptDataRepository::toContext);
    }

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
        attempt.setExercise(exerciseRepository.getReferenceById(exerciseId));
        attempt.setUser(userRepository.getReferenceById(userId));
        attempt.setCourse(courseId == null ? null : courseRepository.getReferenceById(courseId));
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
                exercise.getStrategyId(),
                exercise.getStages() == null ? List.of() : List.copyOf(exercise.getStages()),
                exercise.getOptions().isPreferDecisionTreeBasedSupplementaryEnabled());
    }

    private static @NotNull GradePassbackTargetData toGradePassbackTarget(
            @NotNull GradePassbackTargetRow row) {
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
