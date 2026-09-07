package org.vstu.compprehension.data.exercise;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Упражнение целиком — всё, что о нём знает и показывает система.
 * <p>
 * Заменяет {@code ExerciseEntity} в контракте сервиса. Домен приходит одним
 * идентификатором: из связи читалось только имя, а сама связь ленивая, и обращение
 * к ней за пределами транзакции падало.
 * <p>
 * Поля, оставшиеся в таблице от прежних версий и нигде не читаемые
 * ({@code maxRetries}, {@code hidden}, {@code exerciseType}, {@code language}), сюда
 * не попадают: {@code *Data} описывает то, что действительно используется.
 *
 * @param domainId  идентификатор предметной области, он же её имя
 * @param isPublic  упражнение из глобального пула: видно во всех курсах и правится
 *                  только вне курса
 * @param options   вопреки имени класса, {@code ExerciseOptionsData} — не JPA-сущность,
 *                  а значение из json-колонки {@code options_json}
 */
public record ExerciseData(
        long id,
        @NotNull String name,
        @NotNull String domainId,
        @NotNull String backendId,
        @NotNull String strategyId,
        @NotNull ExerciseOptionsData options,
        @NotNull List<ExerciseStageData> stages,
        @NotNull List<String> tags,
        boolean isPublic) {
}
