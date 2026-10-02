package org.vstu.compprehension.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.vstu.compprehension.authorization.AbstractAuthorizationTest;
import org.vstu.compprehension.infrastructure.TestData;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StrategySettingsControllerTest extends AbstractAuthorizationTest {

    @Autowired private ObjectMapper objectMapper;

    /** Настройки стратегии, заданные в форме упражнения, сохраняются через карточку и возвращаются с ней. */
    @Test
    void strategySettingsAreSavedAndReturnedWithExerciseCard() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var card = (ObjectNode) objectMapper.readTree(mockMvc.perform(get("/api/exercise")
                        .param("id", String.valueOf(TestData.Exercises.GLOBAL_POOL_ID)))
                .andReturn().getResponse().getContentAsString());
        card.withObject("options").set("strategySettings",
                objectMapper.readTree("{\"correctAnswerClarification\": {\"mode\": \"ALWAYS\"}}"));

        // Act.
        var saved = mockMvc.perform(post("/api/exercise")
                .contentType(MediaType.APPLICATION_JSON)
                .content(card.toString()));

        // Assert.
        saved.andExpect(status().isOk());
        mockMvc.perform(get("/api/exercise").param("id", String.valueOf(TestData.Exercises.GLOBAL_POOL_ID)))
                .andExpect(jsonPath("$.options.strategySettings.correctAnswerClarification.mode").value("ALWAYS"))
                .andExpect(jsonPath("$.options.strategySettings.correctAnswerClarification.streakLength").value(7));
    }

    /** Список стратегий отдаёт форму настроек и значения по умолчанию как обычный JSON. */
    @Test
    void strategiesListDescribesSettingsAsJson() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/refTables/strategies"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + TestData.Exercises.STRATEGY_ID + "')].settings.fields[0].kind",
                        hasItem("GROUP")))
                .andExpect(jsonPath("$[?(@.id == '" + TestData.Exercises.STRATEGY_ID + "')]"
                        + ".settings.defaults.correctAnswerClarification.mode", hasItem("NEVER")));
    }
}
