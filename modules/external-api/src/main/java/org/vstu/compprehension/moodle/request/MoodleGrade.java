package org.vstu.compprehension.moodle.request;

/**
 * Оценка студента: нормализованное значение {@code grade} [0..1] и максимум шкалы
 *
 * @param grade    нормализованная оценка [0..1]
 * @param maxScore максимальная оценка (шкала колонки журнала)
 */
public record MoodleGrade(double grade, double maxScore) {
    /** Оценка в шкале Moodle */
    public double rawScore() {
        return grade * maxScore;
    }
}
