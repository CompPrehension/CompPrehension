package org.vstu.compprehension.models.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.DisplayingFeedbackType;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;

public interface AbstractStrategy {
    @NotNull String getStrategyId();

    QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt);

    DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question);

    FeedbackType determineFeedbackType(QuestionEntity question);

    float grade(ExerciseAttemptEntity exerciseAttempt);

    Decision decide(ExerciseAttemptEntity exerciseAttempt);
}
