package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.EnumData.Language;

/**
 * Всё, что нужно знать о попытке, чтобы сгенерировать в ней очередной вопрос.
 * <p>
 * Отдельно от {@link QuestionAttemptContextData}: тот отвечает на вопросы о попытке,
 * в которой вопрос уже задан, и настроек упражнения в нём нет — они нужны только при
 * генерации. Один класс на оба случая означал бы, что половина полей заполнена
 * через раз.
 *
 * @param domainId идентификатор предметной области, он же её имя
 */
public record AttemptGenerationContextData(
        long attemptId,
        @NotNull String domainId,
        @NotNull String strategyId,
        @NotNull ExerciseOptionsData exerciseOptions,
        @NotNull Language userLanguage) {
}
