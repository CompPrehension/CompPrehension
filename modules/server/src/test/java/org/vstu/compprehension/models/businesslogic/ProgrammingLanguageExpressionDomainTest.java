package org.vstu.compprehension.models.businesslogic;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.BackendFactory;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProgrammingLanguageExpressionDomainTest {

    @Autowired
    ProgrammingLanguageExpressionDomain domain;

    @Autowired
    BackendFactory backendFactory;

    @Test
    public void testName() {
        assertEquals(domain.getName(), "ProgrammingLanguageExpressionDomain");
    }

    @Test
    public void testLaws() {
        assertNotNull(domain.getPositiveLaw("single_token_binary_execution"));
        assertTrue(domain.getPositiveLaw("single_token_binary_execution").isPositiveLaw());
    }

    @Test
    public void TestSupplementaryConfig() {
        for (val entry : domain.getSupplementaryConfig().entrySet()) {
            String supName = entry.getKey();
            HashSet<String> supNameSet = new HashSet<>();
            supNameSet.add(supName);
            supNameSet.add("supplementary");
            Question sup = domain.findQuestion(new ArrayList<>(), supNameSet, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
            assertNotNull(sup, supName);
            for (AnswerObjectEntity answer : sup.getAnswerObjects()) {
                assertTrue(entry.getValue().containsKey(answer.getDomainInfo()), answer.getDomainInfo());
            }
            for (val answer : entry.getValue().entrySet()) {
                for (val transition : answer.getValue()) {
                    assertTrue(domain.getSupplementaryConfig().containsKey(transition.question), transition.question);
                }
            }
        }
    }

    List<BackendFactEntity> createStatement(List<String> expression, List<String> isOperator) {
        List<BackendFactEntity> facts = new ArrayList<>();
        for (int i = 0; i < expression.size(); ++i) {
            facts.add(new BackendFactEntity(isOperator.get(i),"",expression.get(i)));
        }
        return facts;
    }

    boolean validateQuestionByQuestionRequest(Question q, QuestionRequest qr) {
        return
                q.concepts == null
                        ||
                // all TargetConcepts are in q
                (q.concepts.containsAll(qr.getTargetConcepts().stream().map(Concept::getName).collect(Collectors.toList())))
                        &&
                // none of DeniedConcepts is in q
                qr.getDeniedConcepts().stream().map(Concept::getName).collect(Collectors.toList()).stream().noneMatch(s -> q.concepts.contains(s));
    }

    @Test
    public void testQuestionGeneration() throws Exception {
        List<Tag> tags = Stream.of("basics", "operators", "order", "evaluation", "C++")
                .map(t -> domain.getTag(t))
                .filter(Objects::nonNull)
                .toList();

        QuestionRequest qr = new QuestionRequest();
        qr.setTargetConcepts(List.of(
                domain.getConcept("precedence"),
                domain.getConcept("SystemIntegrationTest")
        ));
        qr.setAllowedConcepts(List.of(
                domain.getConcept("operator_binary_+"),
                domain.getConcept("operator_binary_*")
        ));
        qr.setDeniedConcepts(List.of(
                domain.getConcept("associativity")
        ));
        qr.setTargetLaws(List.of());
        qr.setDeniedLaws(List.of());

        Question q = domain.makeQuestion(null, qr, tags, Language.ENGLISH);
        assertTrue(validateQuestionByQuestionRequest(q, qr), q.getQuestionName());
        assertEquals("<p>Press the operators in the expression in the order they are evaluated</p><div class='comp-ph-question'><p class='comp-ph-expr'><span data-comp-ph-pos='1' class='comp-ph-expr-const' data-comp-ph-value=''>a</span><span data-comp-ph-pos='2' id='answer_0' class='comp-ph-expr-op-btn' data-comp-ph-value=''>==</span><span data-comp-ph-pos='3' class='comp-ph-expr-const' data-comp-ph-value=''>b</span><span data-comp-ph-pos='4' id='answer_1' class='comp-ph-expr-op-btn' data-comp-ph-value=''><</span><span data-comp-ph-pos='5' class='comp-ph-expr-const' data-comp-ph-value=''>c</span><!-- Original expression: a == b < c --></p></div>",
                q.getQuestionText().getText());
        QuestionRequest qr2 = new QuestionRequest();
        qr2.setTargetConcepts(List.of(
                domain.getConcept("associativity"),
                domain.getConcept("SystemIntegrationTest")
        ));
        qr2.setAllowedConcepts(List.of(
                domain.getConcept("operator_binary_+")
        ));
        qr2.setDeniedConcepts(List.of(
                domain.getConcept("precedence"),
                domain.getConcept("operator_evaluating_left_operand_first")
        ));
        qr2.setTargetLaws(List.of());
        qr2.setDeniedLaws(List.of());

        q = domain.makeQuestion(null, qr2, tags, Language.ENGLISH);
        assertTrue(validateQuestionByQuestionRequest(q, qr2), q.getQuestionName());
        //        assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml(createStatement(List.of("a", "+", "b", "+", "c"), List.of("", "operator", "", "operator", ""))), domain.makeQuestion(qr2, tags, Language.ENGLISH).getQuestionText().getText());

        QuestionRequest qr3 = new QuestionRequest();
        qr3.setTargetConcepts(List.of(
                domain.getConcept("associativity"),
                domain.getConcept("precedence"),
                domain.getConcept("SystemIntegrationTest")
        ));
        qr3.setAllowedConcepts(List.of(
                domain.getConcept("operator_binary_*"),
                domain.getConcept("operator_binary_+")
        ));
        qr3.setDeniedConcepts(List.of(
                domain.getConcept("operator_evaluating_left_operand_first")
        ));
        qr3.setTargetLaws(List.of());
        qr3.setDeniedLaws(List.of());
        q = domain.makeQuestion(null, qr3, tags, Language.ENGLISH);
        assertTrue(validateQuestionByQuestionRequest(q, qr3), q.getQuestionName());
//        assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml(createStatement(List.of("a", "+", "b", "+", "c", "--"), List.of("", "operator", "", "operator", "", "operator"))), );

        QuestionRequest qr4 = new QuestionRequest();
        qr4.setTargetConcepts(List.of(
                domain.getConcept("SystemIntegrationTest")
        ));
        qr4.setAllowedConcepts(List.of());
        qr4.setDeniedConcepts(List.of(
                domain.getConcept("associativity"),
                domain.getConcept("precedence"),
                domain.getConcept("type"),
                domain.getConcept("SystemIntegrationTest")
        ));
        qr4.setTargetLaws(List.of());
        qr4.setDeniedLaws(List.of());
        assertEquals("Choose associativity of operator binary +",
                domain.makeQuestion(null, qr4, tags, Language.ENGLISH).getQuestionText().getText());
    }

    @Test
    public void testQuestionSolve() throws Exception {
        List<Tag> tags = Stream.of("basics", "operators", "order", "evaluation", "C++")
                .map(t -> domain.getTag(t))
                .filter(Objects::nonNull)
                .toList();

        QuestionRequest qr = new QuestionRequest();
        qr.setTargetConcepts(List.of(
                domain.getConcept("precedence"),
                domain.getConcept("SystemIntegrationTest")
        ));
        qr.setAllowedConcepts(List.of(
                domain.getConcept("operator_binary_+"),
                domain.getConcept("operator_binary_*")
        ));
        qr.setDeniedConcepts(List.of(
                domain.getConcept("associativity"),
                domain.getConcept("operator_evaluating_left_operand_first")
        ));
        qr.setTargetLaws(List.of(
                domain.getNegativeLaw("error_base_higher_precedence_right")
        ));
        qr.setDeniedLaws(List.of(
                domain.getNegativeLaw("error_base_student_error_strict_operands_order")
        ));
        Question question = domain.makeQuestion(null, qr, tags, Language.ENGLISH);
        assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml(createStatement(List.of("a", "==", "b", "<", "c"), List.of("", "operator", "", "operator", ""))), question.getQuestionText().getText());

        Backend backend = backendFactory.getBackend("Jena");
        Collection<Fact> solution = backend.solve(
                domain.getQuestionLaws(question.getQuestionDomainType(), tags),
                question.getStatementFacts(),
                new ReasoningOptions(false, domain.getSolutionVerbs(question.getQuestionDomainType(), List.of()), null));
        assertFalse(solution.isEmpty());
        question.getQuestionData().setSolutionFacts(Fact.factsToEntities(solution));

        Set<String> init = domain.possibleViolations(question, null);
        assertEquals(2, init.size());
        assertTrue(init.contains("error_base_higher_precedence_right"));
        assertTrue(init.contains("error_base_student_error_early_finish"));
    }

    @Test
    public void TestViolation() {
        List<Tag> tags = Stream.of("basics", "operators", "order", "evaluation", "C++")
                .map(t -> domain.getTag(t))
                .filter(Objects::nonNull)
                .toList();

        QuestionRequest qr = new QuestionRequest();
        qr.setTargetConcepts(List.of(
                domain.getConcept("associativity"),
                domain.getConcept("precedence"),
                domain.getConcept("SystemIntegrationTest")
        ));
        qr.setAllowedConcepts(List.of(
                domain.getConcept("operator_binary_*"),
                domain.getConcept("operator_binary_+")
        ));
        qr.setDeniedConcepts(List.of(
                domain.getConcept("operator_evaluating_left_operand_first")
        ));
        qr.setTargetLaws(List.of(
                domain.getNegativeLaw("error_base_higher_precedence_right"),
                domain.getNegativeLaw("error_base_same_precedence_left_associativity_left")
        ));
        Question q = domain.makeQuestion(null, qr, tags, Language.ENGLISH);
        Backend backend = backendFactory.getBackend("Jena");
        Collection<Fact> solution = backend.solve(
                domain.getQuestionLaws(q.getQuestionDomainType(), tags),
                q.getStatementFacts(),
                new ReasoningOptions(false, domain.getSolutionVerbs(q.getQuestionDomainType(), List.of()), null));
        assertFalse(solution.isEmpty());
        q.getQuestionData().setSolutionFacts(Fact.factsToEntities(solution));

        Set<String> init = domain.possibleViolations(q, null);
        assertEquals(3, init.size());
        assertTrue(init.contains("error_base_higher_precedence_right"));
        assertTrue(init.contains("error_base_same_precedence_left_associativity_left"));
        assertTrue(init.contains("error_base_student_error_early_finish"));

        init = domain.possibleViolations(q, new ArrayList<>());
        assertEquals(3, init.size());
        assertTrue(init.contains("error_base_higher_precedence_right"));
        assertTrue(init.contains("error_base_same_precedence_left_associativity_left"));
        assertTrue(init.contains("error_base_student_error_early_finish"));

        ResponseEntity response0 = new ResponseEntity();
        response0.setLeftAnswerObject(q.getAnswerObject(0));
        response0.setRightAnswerObject(q.getAnswerObject(0));

        ResponseEntity response1 = new ResponseEntity();
        response1.setLeftAnswerObject(q.getAnswerObject(1));
        response1.setRightAnswerObject(q.getAnswerObject(1));

        ResponseEntity response2 = new ResponseEntity();
        response2.setLeftAnswerObject(q.getAnswerObject(2));
        response2.setRightAnswerObject(q.getAnswerObject(2));

        init = domain.possibleViolations(q, new ArrayList<>(List.of(response0)));
        assertEquals(2, init.size());
        assertTrue(init.contains("error_base_higher_precedence_right"));
        assertTrue(init.contains("error_base_student_error_early_finish"));

        init = domain.possibleViolations(q, new ArrayList<>(List.of(response0, response2)));
        assertEquals(0, init.size());

        init = domain.possibleViolations(q, new ArrayList<>(List.of(response2)));
        assertEquals(2, init.size());
        assertTrue(init.contains("error_base_same_precedence_left_associativity_left"));
        assertTrue(init.contains("error_base_student_error_early_finish"));

        init = domain.possibleViolations(q, new ArrayList<>(List.of(response2, response0)));
        assertEquals(0, init.size());

        init = domain.possibleViolations(q, new ArrayList<>(List.of(response2, response0, response1)));
        assertEquals(0, init.size());
    }

    @Test
    public void TestUneval() {
        List<Tag> tags = Stream.of("basics", "operators", "order", "evaluation", "C++")
                .map(t -> domain.getTag(t))
                .filter(Objects::nonNull)
                .toList();

        QuestionRequest qr = new QuestionRequest();
        qr.setTargetConcepts(List.of(
                domain.getConcept("SystemIntegrationTest")
        ));
        qr.setAllowedConcepts(List.of());
        qr.setDeniedConcepts(List.of());
        qr.setTargetLaws(List.of(
                domain.getNegativeLaw("error_base_student_error_unevaluated_operand")
        ));
        Question q = domain.makeQuestion(null, qr, tags, Language.ENGLISH);
        Backend backend = backendFactory.getBackend("Jena");
        Collection<Fact> solution = backend.solve(
                domain.getQuestionLaws(q.getQuestionDomainType(), tags),
                q.getStatementFacts(),
                new ReasoningOptions(false, domain.getSolutionVerbs(q.getQuestionDomainType(), List.of()), null));
        assertFalse(solution.isEmpty());
        q.getQuestionData().setSolutionFacts(Fact.factsToEntities(solution));

        Set<String> init = domain.possibleViolations(q, null);
        assertTrue(init.contains("error_base_student_error_unevaluated_operand"));
    }

    //@Test
    public void TestRandom() {
        List<Tag> tags = Stream.of("basics", "operators", "order", "evaluation", "C++")
                .map(t -> domain.getTag(t))
                .filter(Objects::nonNull)
                .toList();

        QuestionRequest qr = new QuestionRequest();
        qr.setTargetConcepts(List.of(
        ));
        qr.setAllowedConcepts(List.of());
        qr.setDeniedConcepts(List.of(
                domain.getConcept("SystemIntegrationTest")
        ));
        qr.setTargetLaws(List.of(
        ));
        Question q = domain.makeQuestion(null, qr, tags, Language.ENGLISH);

        assertFalse(q.getSolutionFacts().isEmpty());
    }
}