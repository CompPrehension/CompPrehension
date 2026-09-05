package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Попытка прохождения упражнения вместе с заданными вопросами и взаимодействиями —
 * ровно то, что нужно стратегиям для {@code generateQuestionRequest}, {@code grade}
 * и {@code decide}.
 * <p>
 * Собирается сервисом фиксированным числом запросов (см.
 * {@code ExerciseAttemptService#getAttemptWithQuestions}), а не обходом ленивого графа,
 * поэтому стоимость не зависит от количества вопросов в попытке.
 *
 * @param questions в порядке возрастания id, то есть в порядке выдачи вопросов
 */
public record ExerciseAttemptWithQuestionsData(
        long id,
        long userId,
        @NotNull AttemptExerciseData exercise,
        @NotNull List<AttemptQuestionData> questions) {
}
