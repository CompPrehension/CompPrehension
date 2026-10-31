package org.vstu.compprehension.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.vstu.compprehension.authorization.AbstractAuthorizationTest;
import org.vstu.compprehension.infrastructure.TestData;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseSettingsFormControllerTest extends AbstractAuthorizationTest {

    private static final String ADAPTIVE_STRATEGY_ID = "GradeConfidenceBaseStrategy";

    @Autowired private ObjectMapper objectMapper;

    /** Настройки реакции на ответы, заданные в форме упражнения, сохраняются через карточку и возвращаются с ней. */
    @Test
    void answerReactionIsSavedAndReturnedWithExerciseCard() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var card = loadCard();
        card.withObject("options").set("answerReaction",
                objectMapper.readTree("{\"correctAnswerClarification\": {\"mode\": \"ALWAYS\"}}"));

        // Act.
        var saved = saveCard(card);

        // Assert.
        saved.andExpect(status().isOk());
        mockMvc.perform(get("/api/exercise").param("id", String.valueOf(TestData.Exercises.GLOBAL_POOL_ID)))
                .andExpect(jsonPath("$.options.answerReaction.correctAnswerClarification.mode").value("ALWAYS"))
                .andExpect(jsonPath("$.options.answerReaction.correctAnswerClarification.streakLength").value(7));
    }

    /** Стратегия, выбранная для этапа в форме упражнения, сохраняется через карточку вместе со своими настройками. */
    @Test
    void stageStrategyIsSavedAndReturnedWithExerciseCard() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var card = loadCard();
        var stage = (ObjectNode) card.get("stages").get(0);
        stage.put("strategyId", ADAPTIVE_STRATEGY_ID);
        stage.set("strategySettings", objectMapper.createObjectNode());

        // Act.
        var saved = saveCard(card);

        // Assert.
        saved.andExpect(status().isOk());
        mockMvc.perform(get("/api/exercise").param("id", String.valueOf(TestData.Exercises.GLOBAL_POOL_ID)))
                .andExpect(jsonPath("$.stages[0].strategyId").value(ADAPTIVE_STRATEGY_ID))
                .andExpect(jsonPath("$.stages[0].strategySettings").isMap());
    }

    /** Форма реакции на ответы отдаётся с полями и значениями по умолчанию как обычный JSON. */
    @Test
    void answerReactionFormIsDescribedAsJson() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/refTables/answerReactionSettings"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[0].kind").value("CHOICE"))
                .andExpect(jsonPath("$.defaults.correctAnswerClarification.mode").value("NEVER"));
    }

    /** Список стратегий отдаёт форму настроек каждой, даже если собственных настроек у стратегии нет. */
    @Test
    void strategiesListDescribesSettingsAsJson() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/refTables/strategies"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + TestData.Exercises.STRATEGY_ID + "')].settings.defaults",
                        hasItem(Map.of())));
    }

    private ObjectNode loadCard() throws Exception {
        return (ObjectNode) objectMapper.readTree(mockMvc.perform(get("/api/exercise")
                        .param("id", String.valueOf(TestData.Exercises.GLOBAL_POOL_ID)))
                .andReturn().getResponse().getContentAsString());
    }

    private ResultActions saveCard(ObjectNode card) throws Exception {
        return mockMvc.perform(post("/api/exercise")
                .contentType(MediaType.APPLICATION_JSON)
                .content(card.toString()));
    }
}
