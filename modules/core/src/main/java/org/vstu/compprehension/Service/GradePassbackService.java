package org.vstu.compprehension.Service;


/**
 * Out-port: отправка итоговой оценки в gradebook.
 * Реализация выбирает конкретный механизм (LTI AGS, Moodle WS и т.д.) на основе данных attempt'а.
 */
public interface GradePassbackService {
    /**
     * Выставить оценку за попытку во внешнюю систему.
     * <p>
     * Принимает идентификатор, а не сущность: реализация всё равно перечитывает попытку
     * — метод асинхронный и выполняется в своей транзакции, так что переданный объект
     * ей не годится.
     */
    void passGrade(long attemptId, double grade);
}
