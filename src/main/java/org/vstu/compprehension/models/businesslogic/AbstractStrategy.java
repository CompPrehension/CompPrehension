package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.entities.EnumData.DisplayingFeedbackType;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;

public abstract class AbstractStrategy {
    public abstract QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt);

    public abstract DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question);

    public abstract FeedbackType determineFeedbackType(QuestionEntity question);

    public abstract float grade(ExerciseAttemptEntity exerciseAttempt);
}
