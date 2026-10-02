package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.businesslogic.domains.ControlFlowDTDomain;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.DomainDto;
import org.vstu.compprehension.frontend.dto.StrategyDto;
import org.vstu.compprehension.frontend.dto.StrategySettingsDto;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceTableFrontendServiceTest extends AbstractIntegrationTest {

    private static final String HIDDEN_STRATEGY_ID = "GradeConfidenceBaseStrategy_Manual50Autogen50";

    @Autowired private ReferenceTableFrontendService service;

    /** Стратегии: только видимые пользователю. */
    @Test
    void getStrategiesListsOnlyVisibleOnes() {
        // Act.
        var strategies = service.getStrategies(Language.ENGLISH);

        // Assert.
        assertTrue(strategies.stream().anyMatch(s -> s.getId().equals(TestData.Exercises.STRATEGY_ID)));
        assertTrue(strategies.stream().noneMatch(s -> s.getId().equals(HIDDEN_STRATEGY_ID)));
        assertTrue(strategies.stream().allMatch(s -> s.getOptions().isVisibleToUser()));
        assertTrue(strategies.stream().noneMatch(s -> s.getDisplayName().isBlank()));
    }

    /** Названия стратегий локализованы. */
    @Test
    void getStrategiesAreLocalized() {
        // Act.
        var russian = find(service.getStrategies(Language.RUSSIAN), TestData.Exercises.STRATEGY_ID);
        var english = find(service.getStrategies(Language.ENGLISH), TestData.Exercises.STRATEGY_ID);

        // Assert.
        assertEquals("Статическая стратегия", russian.getDisplayName());
        assertEquals("Static strategy", english.getDisplayName());
    }

    /** Домены: все зарегистрированные, с деревьями настроек. */
    @Test
    void getDomainsListsRegisteredDomainsWithTrees() {
        // Act.
        var domains = service.getDomains(Language.ENGLISH);

        // Assert.
        assertTrue(domains.stream().anyMatch(d -> d.getId().equals(ControlFlowDTDomain.DOMAIN_ID)));
        assertTrue(domains.stream().noneMatch(d -> d.getDisplayName().isBlank()));
        var expression = findDomain(domains, TestData.Exercises.DOMAIN_ID);
        assertTrue(expression.getTags().contains("C++"));
        assertFalse(expression.getConcepts().isEmpty());
        assertFalse(expression.getSkills().isEmpty());
    }

    /** Названия доменов локализованы. */
    @Test
    void getDomainsAreLocalized() {
        // Act.
        var russian = findDomain(service.getDomains(Language.RUSSIAN), TestData.Exercises.DOMAIN_ID);
        var english = findDomain(service.getDomains(Language.ENGLISH), TestData.Exercises.DOMAIN_ID);

        // Assert.
        assertNotEquals(russian.getDisplayName(), english.getDisplayName());
    }

    /** Бэкенды: по одному на каждый домен. */
    @Test
    void getBackendIdsCoverEveryDomain() {
        // Act.
        var backendIds = service.getBackendIds();

        // Assert.
        assertTrue(backendIds.contains("DTReasoner"));
        assertFalse(backendIds.isEmpty());
    }

    /** Стратегия описывает форму своих настроек на языке пользователя и значения для нового упражнения. */
    @Test
    void getStrategiesDescribeSettingsForm() {
        // Act.
        var strategy = find(service.getStrategies(Language.RUSSIAN), TestData.Exercises.STRATEGY_ID);

        // Assert.
        assertEquals(List.of(new StrategySettingsDto.Group("correctAnswerClarification", "Уточнять рассуждение при верном ответе",
                List.of(new StrategySettingsDto.Choice("mode", "Режим", List.of(
                                new StrategySettingsDto.Option("NEVER", "Никогда"),
                                new StrategySettingsDto.Option("ALWAYS", "Всегда"),
                                new StrategySettingsDto.Option("UNTIL_STREAK", "До серии верных ответов"))),
                        new StrategySettingsDto.Numeric("streakLength", "Длина серии верных ответов", 1, Integer.MAX_VALUE)))),
                strategy.getSettings().fields());
        assertEquals(Map.of("correctAnswerClarification", Map.of("mode", "NEVER", "streakLength", 7)),
                strategy.getSettings().defaults());
    }

    private static StrategyDto find(List<StrategyDto> strategies, String id) {
        return strategies.stream().filter(s -> s.getId().equals(id)).findFirst().orElseThrow();
    }

    private static DomainDto findDomain(List<DomainDto> domains, String id) {
        return domains.stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
    }
}
