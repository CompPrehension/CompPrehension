package org.vstu.compprehension.data.exercise;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Правка карточки упражнения: поля, которые редактирует преподаватель.
 * <p>
 * Перечислены именно изменяемые поля, а не упражнение целиком: запись агрегата целиком
 * стирает то, чего в данных нет, — например, признак публичности, который карточкой не
 * управляется и меняется только переносом упражнения между курсом и глобальным пулом.
 */
public record ExerciseCardUpdateData(
        long id,
        @NotNull String name,
        @NotNull String domainId,
        @NotNull String backendId,
        @NotNull String strategyId,
        @NotNull ExerciseOptionsData options,
        @NotNull List<ExerciseStageData> stages,
        @NotNull List<String> tags) {
}
