package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.Decision;

import java.util.List;

public record AnswerFeedbackData(
        @NotNull QuestionData question,
        @Nullable List<Message> messages,
        @Nullable List<ResponseData> correctAnswers,
        boolean correct,
        int stepsLeft,
        float grade,
        @Nullable Decision strategyDecision) {

    public enum MessageType { ERROR, SUCCESS }

    public record Message(@NotNull MessageType type,
                          @NotNull String text,
                          @Nullable List<Knowledge> knowledge) {

        public static @NotNull Message success(@NotNull String text, @Nullable List<Knowledge> knowledge) {
            return new Message(MessageType.SUCCESS, text, knowledge);
        }

        public static @NotNull Message error(@NotNull String text, @Nullable List<Knowledge> knowledge) {
            return new Message(MessageType.ERROR, text, knowledge);
        }
    }

    /** Знание домена, о котором сообщение: закон или навык — как в домене. */
    public record Knowledge(@NotNull String name, boolean canCreateSupplementaryQuestion) {
    }
}
