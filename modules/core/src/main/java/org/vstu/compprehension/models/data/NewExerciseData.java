package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Упражнение, которого ещё нет в базе.
 * <p>
 * Отличается от {@link ExerciseData} отсутствием идентификатора — и только им. Тип
 * заведён именно ради этого: идентификатор появляется при вставке, и «тот же класс,
 * но с нулём в id» означал бы, что вызывающий обязан помнить, заполнено поле или нет.
 */
public record NewExerciseData(
        @NotNull String name,
        @NotNull String domainId,
        @NotNull String backendId,
        @NotNull String strategyId,
        @NotNull ExerciseOptionsData options,
        @NotNull List<ExerciseStageData> stages,
        @NotNull List<String> tags,
        boolean isPublic) {
}
