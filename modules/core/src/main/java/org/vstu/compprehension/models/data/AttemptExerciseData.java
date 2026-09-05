package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Упражнение в объёме, нужном стратегиям.
 *
 * @param stages вопреки имени класса, {@code ExerciseStageData} — не JPA-сущность,
 *               а значение, разобранное из json-колонки {@code stages_json}. Обращение
 *               к нему не ходит в базу, поэтому он передаётся как есть; переименование
 *               этого типа — отдельная задача.
 */
public record AttemptExerciseData(
        long id,
        @NotNull String domainName,
        @NotNull List<ExerciseStageData> stages,
        @NotNull List<String> tags) {
}
