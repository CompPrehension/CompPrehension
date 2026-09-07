package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.enums.InteractionType;

import java.util.List;

/**
 * Взаимодействие студента с вопросом, которое надо записать.
 * <p>
 * Оценки здесь нет: её считает стратегия по истории попытки, включая это самое
 * взаимодействие, — то есть уже после того, как оно записано. Выставляется она
 * отдельным вызовом.
 *
 * @param carriedResponseIds ответы прошлых взаимодействий, переезжающие в это; так
 *                           работает показ правильного ответа: он достраивает уже
 *                           данные студентом ответы, а не начинает заново
 * @param answers            ответы, впервые появившиеся в этом взаимодействии
 * @param interactionsLeft   сколько ещё ответов ожидается; отрицательное значение
 *                           означает, что вопрос исчерпан
 */
public record NewInteractionData(
        long questionId,
        @NotNull InteractionType interactionType,
        @NotNull List<Long> carriedResponseIds,
        @NotNull List<SubmittedAnswerData> answers,
        @NotNull List<ViolationData> violations,
        @NotNull List<String> correctLaws,
        int interactionsLeft) {
}
