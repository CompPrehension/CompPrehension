package org.vstu.compprehension.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;

@Data
@Builder
public class FeedbackDto {
    private Float grade;
    private Integer correctSteps;
    private Integer stepsLeft;
    private Integer stepsWithErrors;
    private Long[] violations;
    private Long[][] correctAnswers;
    private Message message;

    public enum MessageType {
        @JsonProperty("ERROR")
        ERROR,
        @JsonProperty("SUCCESS")
        SUCCESS,
    }

    @Data
    @AllArgsConstructor
    public static class Message {
        private MessageType messageType;
        private String[] strings;

        public static Message Success(String[] strings) {
            return new Message(MessageType.SUCCESS, strings);
        }
        public static Message Success(String message) {
            return new Message(MessageType.SUCCESS, new String[]{ message });
        }
        public static Message Error(String[] strings) {
            return new Message(MessageType.ERROR, strings);
        }
        public static Message Error(String message) {
            return new Message(MessageType.ERROR, new String[]{ message });
        }
    }
}

