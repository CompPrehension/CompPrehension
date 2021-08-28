package org.vstu.compprehension.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.EnumData.Decision;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDto {
    private Float grade;
    private Integer correctSteps;
    private Integer stepsLeft;
    private Integer stepsWithErrors;
    private Long[][] correctAnswers;
    private Message[] messages;
    private Decision strategyDecision;

    public enum MessageType {
        @JsonProperty("ERROR")
        ERROR,
        @JsonProperty("SUCCESS")
        SUCCESS,
    }

    @Data
    @AllArgsConstructor
    public static class Message {
        @NotNull private MessageType type;
        @NotNull private String message;
        @Nullable private FeedbackViolationLawDto violationLaw;

        public static Message Success(@NotNull String message) {
            return new Message(MessageType.SUCCESS, message, null);
        }
        public static Message Error(@NotNull String message, @NotNull FeedbackViolationLawDto violationLaw) {
            return new Message(MessageType.ERROR, message, violationLaw);
        }
    }
}

