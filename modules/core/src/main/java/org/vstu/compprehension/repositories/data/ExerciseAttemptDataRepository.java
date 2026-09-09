package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.data.exerciseattempt.AttemptExerciseData;
import org.vstu.compprehension.data.exerciseattempt.AttemptGenerationContextData;
import org.vstu.compprehension.data.exerciseattempt.AttemptOwnerData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.data.exerciseattempt.GradePassbackTargetData;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;
import org.vstu.compprehension.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.enums.AttemptStatus;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.CourseRepository;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.AttemptOwner;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.AttemptSummaryRow;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.GradePassbackTargetRow;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository;
import org.vstu.compprehension.repositories.entity.ExerciseRepository;
import org.vstu.compprehension.repositories.entity.InteractionRepository.InteractionLawRow;
import org.vstu.compprehension.repositories.entity.InteractionRepository.InteractionRow;
import org.vstu.compprehension.repositories.entity.InteractionRepository;
import org.vstu.compprehension.repositories.entity.QuestionRepository;
import org.vstu.compprehension.repositories.entity.UserRepository;
import org.vstu.compprehension.repositories.mappers.AttemptQuestionInteractionMapper;
import org.vstu.compprehension.repositories.mappers.AttemptQuestionMapper;
import org.vstu.compprehension.repositories.mappers.AttemptSummaryMapper;

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
    private final AttemptSummaryMapper attemptSummaryMapper;
    private final Mapper<ExerciseAttemptEntity, QuestionAttemptContextData> questionAttemptContextMapper;
    private final Mapper<GradePassbackTargetRow, GradePassbackTargetData> gradePassbackTargetMapper;
    private final Mapper<AttemptOwner, AttemptOwnerData> attemptOwnerMapper;
    private final Mapper<ExerciseEntity, AttemptExerciseData> attemptExerciseMapper;
    private final AttemptQuestionMapper attemptQuestionMapper;
    private final AttemptQuestionInteractionMapper attemptInteractionMapper;

    @Transactional(readOnly = true)
    public @NotNull ExerciseAttemptWithQuestionsData getAttemptWithQuestions(long attemptId) {
        var attempt = exerciseAttemptRepository.findByIdFetchingExerciseAndDomain(attemptId)
                .orElseThrow(() -> new NoSuchElementException("Exercise attempt " + attemptId + " not found"));
        var exerciseData = attemptExerciseMapper.map(attempt.getExercise());

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
                        Collectors.mapping(row -> attemptInteractionMapper.map(
                                row,
                                violationsByInteraction.getOrDefault(row.getInteractionId(), List.of()),
                                correctLawsByInteraction.getOrDefault(row.getInteractionId(), List.of())
                        ), Collectors.toList())));

        var questionsData = questions.stream()
                .map(q -> attemptQuestionMapper.map(
                        q, interactionsByQuestion.getOrDefault(q.getId(), List.of())))
                .toList();

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
                .map(questionAttemptContextMapper::map);
    }

    @Transactional(readOnly = true)
    public long countQuestionsUpTo(long attemptId, long questionId) {
        return questionRepository.countUpToQuestionInAttempt(attemptId, questionId);
    }

    /** Владелец попытки; пусто, если попытки нет. */
    @Transactional(readOnly = true)
    public @NotNull Optional<AttemptOwnerData> findOwnerByAttemptId(long attemptId) {
        return exerciseAttemptRepository.findOwnerByAttemptId(attemptId)
                .map(attemptOwnerMapper::map);
    }

    /** Владелец попытки, в которой задан вопрос; пусто, если вопрос вне попытки. */
    @Transactional(readOnly = true)
    public @NotNull Optional<AttemptOwnerData> findOwnerByQuestionId(long questionId) {
        return exerciseAttemptRepository.findOwnerByQuestionId(questionId)
                .map(attemptOwnerMapper::map);
    }

    /** Попытка в объёме, который уезжает на фронт; пусто, если попытки нет. */
    @Transactional(readOnly = true)
    public @NotNull Optional<AttemptSummaryData> findSummary(long attemptId) {
        return exerciseAttemptRepository.findSummaryRow(attemptId)
                .map(row -> attemptSummaryMapper.map(row, questionIdsOf(row)));
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
        return row.map(summaryRow -> attemptSummaryMapper.map(summaryRow, questionIdsOf(summaryRow)));
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
                .map(gradePassbackTargetMapper::map);
    }

    /** Итоговая оценка попытки; 0, если оценивать нечего. */
    @Transactional(readOnly = true)
    public double getFinalGrade(long attemptId) {
        return exerciseAttemptRepository.calculateFinalGrade(attemptId).orElse(0.0);
    }

    /** Вопросы попытки в строку не входят и приходят отдельным запросом. */
    private @NotNull List<Long> questionIdsOf(@NotNull AttemptSummaryRow row) {
        return exerciseAttemptRepository.findNonSupplementaryQuestionIds(row.getAttemptId());
    }

    private static Map<Long, List<String>> groupLawNames(List<InteractionLawRow> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                InteractionLawRow::getInteractionId,
                Collectors.mapping(InteractionLawRow::getLawName, Collectors.toList())));
    }
}
