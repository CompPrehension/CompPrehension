package org.vstu.compprehension.integration.moodle;

/**
 * Оценка студента: нормализованное значение {@code grade} ∈ [0..1] и максимум шкалы
 * активности {@code maxScore}. В журнал Moodle пишется значение в шкале — {@link #rawScore()}.
 *
 * @param grade    нормализованная оценка [0..1]
 * @param maxScore максимальная оценка (шкала колонки журнала)
 */
public record MoodleGrade(double grade, double maxScore) {
    /** Оценка в шкале Moodle: {@code grade * maxScore}. */
    public double rawScore() {
        return grade * maxScore;
    }
}
