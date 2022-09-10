package org.vstu.compprehension.models.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.DisplayingFeedbackType;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;

import java.util.ArrayList;

public interface AbstractStrategy {
    @NotNull String getStrategyId();

    QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt);

    DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question);

    FeedbackType determineFeedbackType(QuestionEntity question);

    float grade(ExerciseAttemptEntity exerciseAttempt);

    Decision decide(ExerciseAttemptEntity exerciseAttempt);

    /**
     * Return names of questions were generated within an exercise attempt
     * @param exerciseAttempt attempt
     * @return list of plain names
     */
    default ArrayList<String> listQuestionsOfAttempt(ExerciseAttemptEntity exerciseAttempt) {
        ArrayList<String> deniedQuestions = new ArrayList<>();
        if (exerciseAttempt != null && exerciseAttempt.getQuestions() != null) {
            for (QuestionEntity q : exerciseAttempt.getQuestions()) {
                deniedQuestions.add(q.getQuestionName());
            }
        }
        return deniedQuestions;
    }
}
