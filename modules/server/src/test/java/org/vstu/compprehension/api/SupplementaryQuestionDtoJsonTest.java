package org.vstu.compprehension.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupplementaryQuestionDtoJsonTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    /** Вариант с вопросом сериализуется с тегом QUESTION. */
    @Test
    void questionVariantIsTaggedAsQuestion() {
        // Arrange.
        var dto = new SupplementaryQuestionDto.Question(QuestionDto.builder().questionId(7L).type("ORDER").build());

        // Act.
        var json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        // Assert.
        assertEquals("QUESTION", json.get("kind").asString());
        assertEquals(7L, json.get("question").get("questionId").asLong());
        assertEquals(false, json.has("feedback"));
    }

    /** Вариант с фидбеком сериализуется с тегом FEEDBACK. */
    @Test
    void feedbackVariantIsTaggedAsFeedback() {
        // Arrange.
        var dto = new SupplementaryQuestionDto.Feedback(new SupplementaryFeedbackDto(
                FeedbackDto.Message.Success("done"), SupplementaryFeedbackDto.Action.Finish));

        // Act.
        var json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        // Assert.
        assertEquals("FEEDBACK", json.get("kind").asString());
        assertEquals("FINISH", json.get("feedback").get("action").asString());
        assertEquals("done", json.get("feedback").get("message").get("message").asString());
        assertEquals(false, json.has("question"));
    }
}
