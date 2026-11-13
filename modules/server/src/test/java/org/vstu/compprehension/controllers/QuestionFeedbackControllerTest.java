package org.vstu.compprehension.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.vstu.compprehension.authorization.AbstractAuthorizationTest;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.ExerciseAttemptFrontendService;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.InteractionDto;
import org.vstu.compprehension.infrastructure.TestData;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionFeedbackControllerTest extends AbstractAuthorizationTest {

    @Autowired private ObjectMapper objectMapper;
    @Autowired private ExerciseAttemptFrontendService service;

    /** Сообщение об ошибке приходит со знаниями домена, которые она нарушила: по ним интерфейс предлагает дополнительный вопрос. */
    @Test
    void errorMessageCarriesViolatedKnowledge() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.PARENTHESES_AND_UNARY_MINUS;
        var question = service.generateQuestionByMetadata(bankQuestion.metadataId(), Language.ENGLISH);
        var answerId = bankQuestion.operatorAt(1);
        var mistake = new InteractionDto(question.getQuestionId(), new AnswerDto[] { new AnswerDto(answerId, answerId, true, null) });

        // Act.
        var result = mockMvc.perform(post("/api/question/addQuestionAnswer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mistake)));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].knowledge[0].name").isNotEmpty())
                .andExpect(jsonPath("$.messages[0].knowledge[0].canCreateSupplementaryQuestion").isBoolean());
    }

    /** Дополнительный вопрос запрашивается по нарушенному знанию домена. */
    @Test
    void supplementaryQuestionIsRequestedByViolatedKnowledge() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.PARENTHESES_AND_UNARY_MINUS;
        var question = service.generateQuestionByMetadata(bankQuestion.metadataId(), Language.ENGLISH);
        var answerId = bankQuestion.operatorAt(1);
        var mistake = service.addQuestionAnswer(new InteractionDto(question.getQuestionId(),
                new AnswerDto[] { new AnswerDto(answerId, answerId, true, null) }));
        var violated = mistake.getMessages()[0].getKnowledge().getFirst().getName();

        // Act.
        var result = mockMvc.perform(post("/api/question/generateSupplementaryQuestion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\": " + question.getQuestionId() + ", \"violatedKnowledge\": [\"" + violated + "\"]}"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("QUESTION"));
    }
}
