package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.EnumData.DisplayingFeedbackType;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.ExerciseAttemptEntity;
import com.example.demo.models.entities.QuestionAttemptEntity;

public abstract class AbstractStrategy {
    public abstract QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt);

    public abstract DisplayingFeedbackType determineDisplayingFeedbackType(QuestionAttemptEntity questionAttempt);

    public abstract FeedbackType determineFeedbackType(QuestionAttemptEntity questionAttempt);
}
