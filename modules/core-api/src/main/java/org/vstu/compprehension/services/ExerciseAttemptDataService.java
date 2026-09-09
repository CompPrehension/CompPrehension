package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.data.exerciseattempt.AttemptGenerationContextData;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;

import java.util.Optional;

public interface ExerciseAttemptDataService {
    @NotNull ExerciseAttemptWithQuestionsData getAttemptWithQuestions(long attemptId);

    @NotNull AttemptGenerationContextData getGenerationContext(long attemptId);

    Optional<QuestionAttemptContextData> findQuestionContext(long questionId);

    Language findUserLanguageForQuestion(long questionId);

    Optional<Long> findAttemptIdOfQuestion(long questionId);

    boolean prefersDecisionTreeSupplementary(long questionId);

    void ensureCanAccessAttempt(long userId, long attemptId);

    void ensureCanAccessQuestion(long userId, long questionId);

    Optional<AttemptSummaryData> findSummary(long attemptId);

    Optional<AttemptSummaryData> findIncompleteAttempt(long exerciseId, long userId, @Nullable Long courseId);

    @NotNull AttemptSummaryData createNewAttempt(long exerciseId, long userId, @Nullable Long courseId);

    void ensureAttemptStatus(long attemptId, Decision decision);
}
