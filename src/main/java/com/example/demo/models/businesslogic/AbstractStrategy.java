package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.EnumData.DisplayingFeedbackType;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.ExerciseAttemptEntity;
import com.example.demo.models.entities.QuestionEntity;

public abstract class AbstractStrategy {
    public abstract QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt);

    public abstract DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question);

    public abstract FeedbackType determineFeedbackType(QuestionEntity question);
}
