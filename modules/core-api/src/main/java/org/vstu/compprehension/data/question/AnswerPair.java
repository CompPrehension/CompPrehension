package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;

record AnswerPair(@NotNull AnswerObjectData left, @NotNull AnswerObjectData right) implements AnswerData {

    @Override
    public @NotNull AnswerObjectData getLeftAnswerObject() {
        return left;
    }

    @Override
    public @NotNull AnswerObjectData getRightAnswerObject() {
        return right;
    }
}
