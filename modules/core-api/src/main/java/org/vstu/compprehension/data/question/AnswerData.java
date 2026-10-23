package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;

/**
 * Ответ студента: пара объектов ответа либо выбор значения для объекта ответа.
 */
public sealed interface AnswerData permits AnswerData.Pair, AnswerData.Choice {

    @NotNull AnswerObjectData left();

    @NotNull AnswerObjectData right();

    record Pair(@NotNull AnswerObjectData left, @NotNull AnswerObjectData right) implements AnswerData {
    }

    record Choice(@NotNull AnswerObjectData left, int value) implements AnswerData {

        @Override
        public @NotNull AnswerObjectData right() {
            return left;
        }
    }
}
