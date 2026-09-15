package org.vstu.compprehension.businesslogic.domains.expressiondt;

import its.model.nodes.DecisionTreeElement;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.BankQuestion;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.enums.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.responses;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.cppTags;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.domain;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.expressionQuestion;

class ProgrammingLanguageExpressionDTDomainMetadataTest {

    static Stream<String> expressions() {
        return Stream.concat(
                BANK.stream().map(BankQuestion::expression),
                Stream.of("a && b || c", "x = a ? b : c", "p[i + 1] * f(a, b)", "a + b * c - d", "a * b + c * d",
                        "x = y = z", "-a * b", "f(a) + g(b)"));
    }

    /** Сгенерированные метаданные: сложность в [0, 1] и ненулевые биты. */
    @ParameterizedTest
    @MethodSource("expressions")
    void generatedMetadataIsWellFormed(String expression) {
        // Act.
        var metadata = expressionQuestion(expression).getContent().getMetadata();

        // Assert.
        assertTrue(metadata.getIntegralComplexity() >= 0 && metadata.getIntegralComplexity() <= 1, String.valueOf(metadata.getIntegralComplexity()));
        assertTrue(metadata.getSolutionSteps() >= 1);
        assertTrue(metadata.getSkillBits() != 0);
        assertTrue(metadata.getViolationBits() != 0);
        assertTrue(metadata.getConceptBits() != 0);
        assertEquals(metadata.getConceptBits(), metadata.getTraceConceptBits());
    }

    /** Ошибки, которые находит решатель, объявлены в метаданных, и наоборот для ошибок, известных дереву. */
    @ParameterizedTest
    @MethodSource("expressions")
    void possibleViolationsMatchSolver(String expression) {
        // Arrange.
        var question = expressionQuestion(expression);
        var declared = domain().negativeLawFromBitmask(question.getContent().getMetadata().getViolationBits()).stream()
                .map(Law::getName)
                .collect(Collectors.toSet());
        var knownToTree = lawsKnownToDecisionTrees();

        // Act.
        var solved = new HashSet<String>();
        var finished = false;
        for (var sequence : answerSequences(question)) {
            var result = judge(question, sequence);
            solved.addAll(result.domainNegativeLaws);
            finished |= result.isAnswerCorrect && result.IterationsLeft == 0;
        }

        // Assert.
        assertTrue(finished, "решатель не принял ни одну последовательность как полное решение");
        assertEquals(Set.of(), difference(solved, declared), "ошибки решателя, которых нет в метаданных");
        assertEquals(Set.of(), difference(intersection(declared, knownToTree), solved), "ошибки метаданных, которые решатель не находит");
    }

    /** Умения, которые отмечает решатель, объявлены в метаданных. */
    @ParameterizedTest
    @MethodSource("expressions")
    void solverSkillsAreDeclaredInMetadata(String expression) {
        // Arrange.
        var question = expressionQuestion(expression);
        var declared = baseSkills(domain().skillsFromBitmask(question.getContent().getMetadata().getSkillBits()));

        // Act.
        var solved = new HashSet<String>();
        for (var sequence : answerSequences(question)) {
            for (var name : judge(question, sequence).domainSkills) {
                var skill = domain().getSkill(name);
                assertNotNull(skill, "неизвестное умение " + name);
                solved.addAll(baseSkills(List.of(skill)));
            }
        }

        // Assert.
        assertEquals(Set.of(), difference(solved, declared), "умения решателя, которых нет в метаданных");
    }

    private static Domain.InterpretSentenceResult judge(QuestionData question, List<AnswerObjectData> answers) {
        return domain().judgeQuestion(question, responses(answers), cppTags(), Language.ENGLISH);
    }

    private static Set<String> lawsKnownToDecisionTrees() {
        var laws = new HashSet<String>();
        for (var model : domain().getDomainSolvingModels()) {
            for (var tree : model.getDecisionTrees().values()) {
                collectLaws(tree, laws, new HashSet<>());
            }
        }
        return laws;
    }

    private static void collectLaws(DecisionTreeElement element, Set<String> into, Set<DecisionTreeElement> seen) {
        if (!seen.add(element)) {
            return;
        }
        var law = element.getMetadata().getString("law");
        if (law != null) {
            into.addAll(Arrays.asList(law.split(";")));
        }
        element.getLinkedElements().forEach(linked -> collectLaws(linked, into, seen));
    }

    private static Set<String> baseSkills(List<Skill> skills) {
        var result = new HashSet<String>();
        for (var skill : skills) {
            if (skill.getBaseSkills() == null || skill.getBaseSkills().isEmpty()) {
                result.add(skill.getName());
            } else {
                skill.getBaseSkills().forEach(base -> result.add(base.getName()));
            }
        }
        return result;
    }

    private static <T> Set<T> difference(Set<T> from, Set<T> what) {
        var result = new HashSet<>(from);
        result.removeAll(what);
        return result;
    }

    private static <T> Set<T> intersection(Set<T> a, Set<T> b) {
        var result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    private static List<List<AnswerObjectData>> answerSequences(QuestionData question) {
        var result = new ArrayList<List<AnswerObjectData>>();
        permute(question.getContent().getAnswerObjects(), new ArrayList<>(), result);
        return result;
    }

    private static void permute(List<AnswerObjectData> rest, List<AnswerObjectData> prefix, List<List<AnswerObjectData>> into) {
        for (var answer : rest) {
            var next = new ArrayList<>(prefix);
            next.add(answer);
            into.add(next);
            permute(rest.stream().filter(a -> a != answer).toList(), next, into);
        }
    }
}
