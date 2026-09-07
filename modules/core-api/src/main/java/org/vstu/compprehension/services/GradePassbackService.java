package org.vstu.compprehension.services;


/**
 * Out-port: отправка итоговой оценки в gradebook.
 * Реализация выбирает конкретный механизм (LTI AGS, Moodle WS и т.д.) на основе данных attempt'а.
 */
public interface GradePassbackService {
    /**
     * Выставить оценку за попытку во внешнюю систему.
     */
    void passGrade(long attemptId, double grade);
}
