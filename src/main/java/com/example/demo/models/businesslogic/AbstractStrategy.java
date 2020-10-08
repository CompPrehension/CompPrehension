package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.EnumData.DisplayingFeedbackType;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.ExerciseAttempt;
import com.example.demo.models.entities.QuestionAttempt;

public abstract class AbstractStrategy {
    public abstract QuestionRequest generateQuestionRequest(ExerciseAttempt exerciseAttempt);

    public abstract DisplayingFeedbackType determineDisplayingFeedbackType(QuestionAttempt questionAttempt);

    public abstract FeedbackType determineFeedbackType(QuestionAttempt questionAttempt);
}
