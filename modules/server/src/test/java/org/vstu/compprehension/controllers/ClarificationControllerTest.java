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
import org.vstu.compprehension.services.QuestionDataService;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClarificationControllerTest extends AbstractAuthorizationTest {

    @Autowired private ObjectMapper objectMapper;
    @Autowired private ExerciseAttemptFrontendService service;
    @Autowired private QuestionDataService questionDataService;

    /** Варианты уточняющего вопроса приходят с номерами, и ответ номером выбирает именно этот вариант. */
    @Test
    void clarificationOptionIsChosenByItsNumber() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var questionId = askWhyDivisionIsInteger();
        var shown = objectMapper.readTree(mockMvc.perform(get("/api/question").param("questionId", String.valueOf(questionId)))
                .andReturn().getResponse().getContentAsString());
        var second = shown.path("feedback").path("clarification").path("options").get(1);

        // Act.
        var result = mockMvc.perform(post("/api/question/answerClarification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\": " + questionId + ", \"option\": " + second.path("id").asInt() + "}"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").isNotEmpty());
        var answered = questionDataService.getQuestion(questionId).getInteractions().getLast();
        assertEquals(second.path("id").asInt(), answered.getClarification().chosenReasoning());
    }

    // len(grades) — int, затем total / len(grades) — int: ошибка, которую объясняют два заблуждения.
    private long askWhyDivisionIsInteger() {
        var question = service.generateQuestionByMetadata(TestData.TypeEvaluationBank.AVERAGE_OF_GRADES_METADATA_ID,
                Language.ENGLISH);
        var lengthAsInt = new AnswerDto(TestData.TypeEvaluationBank.LEN_SLOT, TestData.TypeEvaluationBank.INT_TYPE, true, null);
        var afterLength = service.addQuestionAnswer(new InteractionDto(question.getQuestionId(), new AnswerDto[] { lengthAsInt }));
        var divisionAsInt = new AnswerDto(TestData.TypeEvaluationBank.DIV_SLOT, TestData.TypeEvaluationBank.INT_TYPE, true, null);
        service.addQuestionAnswer(new InteractionDto(question.getQuestionId(),
                Stream.concat(Arrays.stream(afterLength.getCorrectAnswers()), Stream.of(divisionAsInt)).toArray(AnswerDto[]::new)));
        return question.getQuestionId();
    }
}
