package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;

/**
 * Ответ студента — пара объектов ответа.
 */
public interface AnswerData {

    @NotNull AnswerObjectData getLeftAnswerObject();

    @NotNull AnswerObjectData getRightAnswerObject();

    static @NotNull AnswerData of(@NotNull AnswerObjectData left, @NotNull AnswerObjectData right) {
        return new AnswerPair(left, right);
    }
}
