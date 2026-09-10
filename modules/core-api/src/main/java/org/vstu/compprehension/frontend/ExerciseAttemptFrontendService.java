package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.*;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;

public interface ExerciseAttemptFrontendService {
    void ensureCanAccessAttempt(long userId, long attemptId);

    void ensureCanAccessQuestion(long userId, long questionId);

    @NotNull SupplementaryFeedbackDto addSupplementaryQuestionAnswer(@NotNull InteractionDto interaction);

    @NotNull FeedbackDto addQuestionAnswer(@NotNull InteractionDto interaction);

    @NotNull QuestionDto generateQuestion(@NotNull Long exAttemptId);

    @NotNull QuestionDto generateQuestionByMetadata(Integer metadataId, Language lang);

    @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(@NotNull Long questionId, @NotNull String[] violationLaws);

    @NotNull QuestionDto getQuestion(@NotNull Long questionId);

    @NotNull FeedbackDto generateNextCorrectAnswer(@NotNull Long questionId);

    @Nullable ExerciseAttemptDto getExerciseAttempt(@NotNull Long attemptId);

    @Nullable ExerciseAttemptDto getExistingExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId, @Nullable Long courseId);

    @NotNull ExerciseAttemptDto createExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId, @Nullable Long courseId);

    @NotNull ExerciseAttemptDto createSolvedExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId, @Nullable Long courseId);
}
