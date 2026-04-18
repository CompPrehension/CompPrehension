package org.vstu.compprehension.service.gradepassback;

import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

/** Реализация отвечает за один механизм отправки оценки (LTI AGS, Moodle WS и т.п.). */
public interface GradePassbackStrategy {
    boolean supports(ExerciseAttemptEntity attempt);
    void passGrade(ExerciseAttemptEntity attempt);

    /**
     * Нормализованная [0.0, 1.0] оценка за попытку
     */
    default double calculateFinalGrade(ExerciseAttemptEntity attempt) {
        var questions = attempt.getQuestions();
        if (questions == null || questions.isEmpty()) return 0.0;

        var lastQuestion = questions.getLast();
        var interactions = lastQuestion.getInteractions();
        if (interactions.isEmpty()) return 0.0;

        var feedback = interactions.getLast().getFeedback();
        if (feedback == null) return 0.0;

        return Math.max(0.0, Math.min(1.0, feedback.getGrade()));
    }
}
