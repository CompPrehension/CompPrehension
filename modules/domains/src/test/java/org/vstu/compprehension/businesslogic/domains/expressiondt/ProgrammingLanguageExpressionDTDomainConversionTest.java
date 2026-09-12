package org.vstu.compprehension.businesslogic.domains.expressiondt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.helpers.meaningtree.MeaningTreeRDFHelper;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.enums.Language;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.iterators.utils.NodeInfo;
import org.vstu.meaningtree.serializers.rdf.RDFDeserializer;
import org.vstu.meaningtree.utils.Label;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.responses;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.MEMBER_ACCESS_PLUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.bankQuestionIn;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.domain;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.endToken;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.expressionQuestion;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.operators;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.token;

class ProgrammingLanguageExpressionDTDomainConversionTest {

    private static final String SUBSCRIPT_AND_CALL = "a[i + 3 * b] = b * 4 + 5";
    private static final String PARENTHESIZED_ARITHMETICS = "(a * (b + c)) * m - ((a + k) * b) + c";
    private static final String STRICT_ORDER = "a && b && (c || d)";

    /** Вопрос из C++-выражения переводится на другой язык и остаётся корректной моделью. */
    @ParameterizedTest
    @EnumSource(value = SupportedLanguage.class, names = {"CPP", "JAVA", "PYTHON"})
    void generatedQuestionIsConvertedToTargetLanguage(SupportedLanguage target) {
        // Act.
        var question = expressionQuestion(SUBSCRIPT_AND_CALL, SupportedLanguage.CPP, target);

        // Assert.
        var content = question.getContent();
        assertTrue(content.getTags().contains(tagOf(target)), content.getTags().toString());
        assertNotNull(new RDFDeserializer().deserializeTree(MeaningTreeRDFHelper.backendFactsToModel(content.getStatementFacts())));
        assertTrue(content.getMetadata().getIntegralComplexity() >= 0 && content.getMetadata().getIntegralComplexity() <= 1);
        assertEquals(6, operators(question).size());
    }

    /** Операторам строгого порядка при переводе назначаются значения операндов. */
    @ParameterizedTest
    @EnumSource(value = SupportedLanguage.class, names = {"CPP", "JAVA", "PYTHON"})
    void strictOrderOperatorsGetRuntimeValues(SupportedLanguage target) {
        // Act.
        var question = expressionQuestion(STRICT_ORDER, SupportedLanguage.CPP, target);

        // Assert.
        var tree = MeaningTreeRDFHelper.backendFactsToMeaningTree(question.getContent().getStatementFacts());
        var hasValues = false;
        for (NodeInfo info : tree) {
            hasValues |= info.node().hasLabel(Label.VALUE);
        }
        assertTrue(hasValues, "у операндов нет значений");
        assertTrue(judge(question, List.of(token(question, 1))).isAnswerCorrect);
    }

    /** Переведённый на Python вопрос решается в порядке вычисления C++. */
    @Test
    void convertedPythonQuestionIsSolvedStepByStep() {
        // Arrange.
        var question = expressionQuestion(SUBSCRIPT_AND_CALL, SupportedLanguage.CPP, SupportedLanguage.PYTHON);

        // Act & Assert.
        assertSolvedStepByStep(question, List.of(5, 3, 1, 10, 12, 8));
    }

    /** Скобки при переводе сохраняют порядок вычисления. */
    @Test
    void parenthesesKeepEvaluationOrderAfterConversion() {
        // Arrange.
        var question = expressionQuestion(PARENTHESIZED_ARITHMETICS, SupportedLanguage.CPP, SupportedLanguage.PYTHON);

        // Act & Assert.
        assertSolvedStepByStep(question, List.of(5, 2, 9, 15, 18, 11, 21));
    }

    /** Вопрос банка отдаётся на языке запрошенного тега. */
    @ParameterizedTest
    @EnumSource(value = SupportedLanguage.class, names = {"CPP", "JAVA", "PYTHON"})
    void bankQuestionIsRenderedInRequestedLanguage(SupportedLanguage target) {
        // Act.
        var question = bankQuestionIn(MEMBER_ACCESS_PLUS, tagOf(target));

        // Assert.
        var text = question.getContent().getQuestionText();
        assertEquals(List.of(tagOf(target)), question.getContent().getTags());
        assertTrue(text.contains(target == SupportedLanguage.CPP ? "Original expression: wp -> sx" : "Original expression: wp . sx"), text);
        assertTrue(text.contains(target == SupportedLanguage.CPP ? "'comp-ph-expr-op-btn'>-></span>" : "'comp-ph-expr-op-btn'>.</span>"), text);
        assertEquals(2, operators(question).size());
    }

    private static void assertSolvedStepByStep(QuestionData question, List<Integer> tokens) {
        var given = new ArrayList<AnswerObjectData>();
        for (var index : tokens) {
            given.add(token(question, index));
            var result = judge(question, given);
            assertTrue(result.isAnswerCorrect, "шаг token_" + index + ": " + result.explanation.toHyperText(Language.ENGLISH).getText());
            assertEquals(tokens.size() - given.size(), result.IterationsLeft);
        }
        given.add(endToken(question));
        assertEquals(0, judge(question, given).IterationsLeft);
    }

    private static Domain.InterpretSentenceResult judge(QuestionData question, List<AnswerObjectData> answers) {
        return domain().judgeQuestion(question, responses(answers), domain().resolveTags(question.getContent().getTags()), Language.ENGLISH);
    }

    private static String tagOf(SupportedLanguage language) {
        return switch (language) {
            case CPP -> "C++";
            case JAVA -> "Java";
            case PYTHON -> "Python";
            default -> throw new IllegalArgumentException(language.toString());
        };
    }
}
