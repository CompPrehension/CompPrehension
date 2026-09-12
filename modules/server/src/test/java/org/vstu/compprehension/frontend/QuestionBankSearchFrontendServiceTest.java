package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.authorization.TestUserService;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.RoleInExercise;
import org.vstu.compprehension.frontend.dto.ExerciseConceptDto;
import org.vstu.compprehension.frontend.dto.ExerciseLawDto;
import org.vstu.compprehension.frontend.dto.ExerciseSkillDto;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchRequestDto;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchStatsDto;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.infrastructure.TestData.ExpressionBank;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class QuestionBankSearchFrontendServiceTest extends AbstractIntegrationTest {

    private static final float MEDIUM = 0.5f;
    private static final List<Float> COMPLEXITY_SWEEP = List.of(0f, 0.25f, 0.5f, 0.75f, 1f);

    private static final Set<Integer> ALL_BANK_IDS = ExpressionBank.ALL.stream()
            .map(ExpressionBank.BankQuestion::metadataId)
            .collect(Collectors.toSet());

    @Autowired private QuestionBankSearchFrontendService service;
    @Autowired private ExerciseAttemptFrontendService attemptService;

    @AfterEach
    void resetCurrentUser() {
        TestUserService.reset();
    }

    // ---- форма ответа ----

    /** Без ограничений виден весь банк. */
    @Test
    void unconstrainedSearchSeesWholeBank() {
        // Act.
        var stats = service.search(request(MEDIUM).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size(), stats.count());
        assertEquals(ExpressionBank.ALL.size(), stats.topRatedCount());
        assertEquals(ALL_BANK_IDS, ids(stats));
        assertTrue(stats.questions().stream().noneMatch(q -> q.name().isBlank()));
    }

    /** Любая сложность даёт согласованный ответ, а все вместе — весь банк. */
    @Test
    void complexitySweepIsConsistentAndCoversWholeBank() {
        // Act.
        var seen = new HashSet<Integer>();
        for (float complexity : COMPLEXITY_SWEEP) {
            var stats = service.search(request(complexity).build());

            // Assert.
            assertWellFormed(stats);
            seen.addAll(ids(stats));
        }
        assertEquals(ALL_BANK_IDS, seen);
    }

    // ---- лимит ----

    /** Лимит режет выдачу, но не счётчики. */
    @Test
    void limitCutsQuestionsButNotCounts() {
        // Act.
        var stats = service.search(request(MEDIUM).limit(2).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size(), stats.count());
        assertEquals(2, stats.questions().size());
        assertTrue(ALL_BANK_IDS.containsAll(ids(stats)));
    }

    /** Нулевой лимит: одни счётчики. */
    @Test
    void zeroLimitReturnsCountsOnly() {
        // Act.
        var stats = service.search(request(MEDIUM).limit(0).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size(), stats.count());
        assertTrue(stats.questions().isEmpty());
    }

    /** Лимит вне [0, 100]. */
    @Test
    void limitOutOfRangeFails() {
        // Act & Assert.
        assertThrows(IllegalArgumentException.class, () -> service.search(request(MEDIUM).limit(101).build()));
        assertThrows(IllegalArgumentException.class, () -> service.search(request(MEDIUM).limit(-1).build()));
    }

    // ---- теги ----

    /** Тег, которым помечен весь банк, ничего не отсекает. */
    @Test
    void tagPresentInEveryQuestionKeepsWholeBank() {
        // Act.
        var stats = service.search(request(MEDIUM).tags(List.of("C++")).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size(), stats.count());
        assertEquals(ALL_BANK_IDS, ids(stats));
    }

    /** Тега нет в банке — всё по нулям. */
    @Test
    void tagAbsentFromBankGivesNothing() {
        // Act.
        var stats = service.search(request(MEDIUM).tags(List.of("Python")).build());

        // Assert.
        assertEquals(0, stats.count());
        assertEquals(0, stats.topRatedCount());
        assertTrue(stats.questions().isEmpty());
    }

    /** Все теги из запроса обязательны. */
    @Test
    void everyRequestedTagIsRequired() {
        // Act.
        var stats = service.search(request(MEDIUM).tags(List.of("C++", "Java")).build());

        // Assert.
        assertEquals(0, stats.count());
        assertTrue(stats.questions().isEmpty());
    }

    /** Неизвестный тег не учитывается. */
    @Test
    void unknownTagIsIgnored() {
        // Act.
        var stats = service.search(request(MEDIUM).tags(List.of("no-such-tag")).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size(), stats.count());
    }

    // ---- концепты ----

    /** Целевой концепт: считаются вопросы с этим оператором. */
    @Test
    void targetConceptCountsQuestionsWithOperator() {
        // Act.
        var stats = service.search(request(MEDIUM).concepts(List.of(concept("operator_->", RoleInExercise.TARGETED))).build());

        // Assert.
        assertEquals(1, stats.count());
        assertTrue(ids(stats).contains(ExpressionBank.MEMBER_ACCESS_PLUS.metadataId()));
    }

    /** Достаточно любого из целевых концептов. */
    @Test
    void anyTargetConceptIsEnough() {
        // Act.
        var stats = service.search(request(MEDIUM).concepts(List.of(
                concept("operator_->", RoleInExercise.TARGETED),
                concept("operator_unary_-", RoleInExercise.TARGETED))).build());

        // Assert.
        assertEquals(3, stats.count());
    }

    /** Групповой концепт раскрывается в дочерние. */
    @Test
    void groupConceptExpandsToChildren() {
        // Act.
        var stats = service.search(request(MEDIUM).concepts(List.of(concept("arithmetics", RoleInExercise.TARGETED))).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size(), stats.count());
    }

    /** Запрещённый концепт убирает вопросы и из счётчика, и из выдачи. */
    @Test
    void forbiddenConceptExcludesQuestions() {
        // Act.
        var stats = service.search(request(MEDIUM).concepts(List.of(concept("operator_unary_-", RoleInExercise.FORBIDDEN))).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size() - 2, stats.count());
        assertFalse(ids(stats).contains(ExpressionBank.ASSIGN_UNARY_MINUS_PLUS.metadataId()));
        assertFalse(ids(stats).contains(ExpressionBank.PARENTHESES_AND_UNARY_MINUS.metadataId()));
    }

    /** Запрещённая группа убирает всё. */
    @Test
    void forbiddenGroupConceptExcludesEverything() {
        // Act.
        var stats = service.search(request(MEDIUM).concepts(List.of(concept("arithmetics", RoleInExercise.FORBIDDEN))).build());

        // Assert.
        assertEquals(0, stats.count());
        assertTrue(stats.questions().isEmpty());
    }

    /** Разрешённый и неизвестный концепты на поиск не влияют. */
    @Test
    void permittedAndUnknownConceptsDoNotFilter() {
        // Act.
        var stats = service.search(request(MEDIUM).concepts(List.of(
                concept("operator_->", RoleInExercise.PERMITTED),
                concept("no_such_concept", RoleInExercise.TARGETED),
                concept("no_such_concept", RoleInExercise.FORBIDDEN))).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size(), stats.count());
        assertEquals(ALL_BANK_IDS, ids(stats));
    }

    // ---- навыки ----

    /** Целевой навык: считаются вопросы, где он нужен. */
    @Test
    void targetSkillCountsQuestionsNeedingIt() {
        // Act.
        var stats = service.search(request(MEDIUM).skills(List.of(skill("order_determined_by_associativity", RoleInExercise.TARGETED))).build());

        // Assert.
        assertEquals(1, stats.count());
        assertTrue(ids(stats).contains(ExpressionBank.MUL_PLUS_MINUS.metadataId()));
    }

    /** Запрещённый навык убирает вопросы. */
    @Test
    void forbiddenSkillExcludesQuestions() {
        // Act.
        var stats = service.search(request(MEDIUM).skills(List.of(skill("order_determined_by_associativity", RoleInExercise.FORBIDDEN))).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size() - 1, stats.count());
        assertFalse(ids(stats).contains(ExpressionBank.MUL_PLUS_MINUS.metadataId()));
    }

    /** Навыка нет ни в одном вопросе. */
    @Test
    void targetSkillAbsentFromBankGivesNothing() {
        // Act.
        var stats = service.search(request(MEDIUM).skills(List.of(skill("order_determined_by_parentheses", RoleInExercise.TARGETED))).build());

        // Assert.
        assertEquals(0, stats.count());
    }

    // ---- законы ----

    /** Целевой закон-ошибка: считаются вопросы, где она возможна. */
    @Test
    void targetLawCountsQuestionsWithViolation() {
        // Act.
        var stats = service.search(request(MEDIUM).laws(List.of(law("error_base_same_precedence_left_associativity_left", RoleInExercise.TARGETED))).build());

        // Assert.
        assertEquals(1, stats.count());
        assertTrue(ids(stats).contains(ExpressionBank.MUL_PLUS_MINUS.metadataId()));
    }

    /** Запрещённый закон-ошибка убирает вопросы. */
    @Test
    void forbiddenLawExcludesQuestions() {
        // Act.
        var stats = service.search(request(MEDIUM).laws(List.of(law("error_base_higher_precedence_left", RoleInExercise.FORBIDDEN))).build());

        // Assert.
        assertEquals(ExpressionBank.ALL.size() - 2, stats.count());
        assertFalse(ids(stats).contains(ExpressionBank.ASSIGN_UNARY_MINUS_PLUS.metadataId()));
        assertFalse(ids(stats).contains(ExpressionBank.PARENTHESES_AND_UNARY_MINUS.metadataId()));
    }

    // ---- использованность ----

    /** Вопрос, выданный в попытке, выпадает из «неиспользованных». */
    @Test
    void questionGivenInAttemptLeavesTopRatedCount() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var before = service.search(request(MEDIUM).build());
        var attempt = attemptService.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        attemptService.generateQuestion(attempt.getAttemptId());

        // Act.
        var after = service.search(request(MEDIUM).build());

        // Assert.
        assertEquals(before.count(), after.count());
        assertEquals(before.topRatedCount() - 1, after.topRatedCount());
    }

    /** Вопрос без попытки использованным не считается. */
    @Test
    void questionWithoutAttemptStaysTopRated() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var before = service.search(request(MEDIUM).build());
        attemptService.generateQuestionByMetadata(ExpressionBank.MEMBER_ACCESS_PLUS.metadataId(), Language.ENGLISH);

        // Act.
        var after = service.search(request(MEDIUM).build());

        // Assert.
        assertEquals(before.topRatedCount(), after.topRatedCount());
    }

    // ---- домен ----

    /** Неизвестный домен. */
    @Test
    void unknownDomainFails() {
        // Act & Assert.
        assertThrows(RuntimeException.class, () -> service.search(request(MEDIUM).domainId("NoSuchDomain").build()));
    }

    // ---- вспомогательное ----

    private static QuestionBankSearchRequestDto.QuestionBankSearchRequestDtoBuilder request(float complexity) {
        return QuestionBankSearchRequestDto.builder()
                .domainId(TestData.Exercises.DOMAIN_ID)
                .complexity(complexity)
                .tags(List.of())
                .laws(List.of())
                .concepts(List.of())
                .skills(List.of())
                .limit(100);
    }

    private static ExerciseConceptDto concept(String name, RoleInExercise kind) {
        return ExerciseConceptDto.builder().name(name).kind(kind).build();
    }

    private static ExerciseSkillDto skill(String name, RoleInExercise kind) {
        return ExerciseSkillDto.builder().name(name).kind(kind).build();
    }

    private static ExerciseLawDto law(String name, RoleInExercise kind) {
        return ExerciseLawDto.builder().name(name).kind(kind).build();
    }

    private static Set<Integer> ids(QuestionBankSearchStatsDto stats) {
        return stats.questions().stream()
                .map(QuestionBankSearchStatsDto.QuestionMetadataDto::metadataId)
                .collect(Collectors.toSet());
    }

    private static void assertWellFormed(QuestionBankSearchStatsDto stats) {
        assertTrue(stats.count() >= 0 && stats.count() <= ExpressionBank.ALL.size());
        assertTrue(stats.topRatedCount() >= 0 && stats.topRatedCount() <= stats.count());
        assertTrue(ALL_BANK_IDS.containsAll(ids(stats)));
        assertTrue(stats.questions().stream().noneMatch(q -> q.name().isBlank()));
    }
}
