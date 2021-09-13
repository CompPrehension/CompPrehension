package org.vstu.compprehension.models.businesslogic;

import lombok.val;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProgrammingLanguageExpressionDomainTest {

    @Autowired
    ProgrammingLanguageExpressionDomain domain;

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
        List<Tag> tags = new ArrayList<>();
        for (String tagString : List.of("basics", "operators", "order", "evaluation", "C++")) {
            Tag tag = new Tag();
            tag.setName(tagString);
            tags.add(tag);
        }

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

        Question q = domain.makeQuestion(qr, tags, Language.ENGLISH);
        assertTrue(validateQuestionByQuestionRequest(q, qr), q.getQuestionName());
        assertEquals("<p>Press the operators in the expression in the order they are evaluated</p><div class='comp-ph-question'><p class='comp-ph-expr'><span data-comp-ph-pos='1' class='comp-ph-expr-const'>a</span><span data-comp-ph-pos='2' id='answer_0' class='comp-ph-expr-op-btn'>==</span><span data-comp-ph-pos='3' class='comp-ph-expr-const'>b</span><span data-comp-ph-pos='4' id='answer_1' class='comp-ph-expr-op-btn'><</span><span data-comp-ph-pos='5' class='comp-ph-expr-const'>c</span><!-- Original expression: a == b < c --></p></div>", domain.makeQuestion(qr, tags, Language.ENGLISH).getQuestionText().getText());

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

        q = domain.makeQuestion(qr2, tags, Language.ENGLISH);
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
        q = domain.makeQuestion(qr3, tags, Language.ENGLISH);
        assertTrue(validateQuestionByQuestionRequest(q, qr3), q.getQuestionName());
//        assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml(createStatement(List.of("a", "+", "b", "+", "c", "--"), List.of("", "operator", "", "operator", "", "operator"))), );

        QuestionRequest qr4 = new QuestionRequest();
        qr4.setTargetConcepts(List.of());
        qr4.setAllowedConcepts(List.of());
        qr4.setDeniedConcepts(List.of(
                domain.getConcept("associativity"),
                domain.getConcept("precedence"),
                domain.getConcept("type"),
                domain.getConcept("SystemIntegrationTest")
        ));
        assertEquals("Choose associativity of operator binary +",
                domain.makeQuestion(qr4, tags, Language.ENGLISH).getQuestionText().getText());
    }

    @Test
    public void testQuestionSolve() throws Exception {
        List<Tag> tags = new ArrayList<>();
        for (String tagString : List.of("basics", "operators", "order", "evaluation", "C++")) {
            Tag tag = new Tag();
            tag.setName(tagString);
            tags.add(tag);
        }

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
                domain.getNegativeLaw("error_base_student_error_strict_operands_order_base")
        ));
        Question question = domain.makeQuestion(qr, tags, Language.ENGLISH);
        assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml(createStatement(List.of("a", "==", "b", "<", "c"), List.of("", "operator", "", "operator", ""))), question.getQuestionText().getText());

        Backend backend = new JenaBackend();
        List<BackendFactEntity> solution = backend.solve(
                domain.getQuestionLaws(question.getQuestionDomainType(), tags),
                question.getStatementFacts(),
                domain.getSolutionVerbs(question.getQuestionDomainType(), new ArrayList<>()));
        assertFalse(solution.isEmpty());
    }
}