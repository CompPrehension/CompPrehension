package org.vstu.compprehension.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;

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

    public enum MessageType {
        @JsonProperty("ERROR")
        ERROR,
        @JsonProperty("SUCCESS")
        SUCCESS,
    }

    @Data
    @AllArgsConstructor
    public static class Message {
        private MessageType type;
        private String message;
        private String[] violationLaws;

        public static Message Success(String message) {
            return new Message(MessageType.SUCCESS, message, null);
        }
        public static Message Error(String message, String[] violationLaws) {
            return new Message(MessageType.ERROR, message, violationLaws);
        }
    }
}

