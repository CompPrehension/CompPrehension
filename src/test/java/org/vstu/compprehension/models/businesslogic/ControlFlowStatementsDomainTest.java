package org.vstu.compprehension.models.businesslogic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ControlFlowStatementsDomainTest {

    @Autowired
    ControlFlowStatementsDomain domain;

    @Test
    public void testName() {
        assertEquals(domain.getName(), "ControlFlowStatementsDomain");
    }

    @Test
    public void testLaws() {
        assertNotNull(domain.getPositiveLaw("entry_point_and_sequence"));
        assertTrue(domain.getPositiveLaw("entry_point_and_sequence").isPositiveLaw());
    }

    @Test
    public void testQuestionSolve() throws Exception {
        List<Tag> tags = new ArrayList<>();
        for (String tagString : List.of("basics", "stmt", "expr", "helper", "C++")) {
            Tag tag = new Tag();
            tag.setName(tagString);
            tags.add(tag);
        }

        QuestionRequest qr = new QuestionRequest();
        qr.setTargetConcepts(List.of(
                domain.getConcept("trace"),
                domain.getConcept("sequence")
        ));
        qr.setAllowedConcepts(List.of(
                domain.getConcept("sequence"),
                domain.getConcept("alternative")
        ));
        qr.setDeniedConcepts(List.of(
                domain.getConcept("loop")
        ));
        Question question = domain.makeQuestion(qr, tags, Language.ENGLISH);

        Backend backend = new JenaBackend();
        List<BackendFactEntity> solution = backend.solve(
                domain.getQuestionLaws(question.getQuestionDomainType(), tags),
                question.getStatementFacts(),
                domain.getSolutionVerbs(question.getQuestionDomainType(), new ArrayList<>()));
        assertFalse(solution.isEmpty());
    }}
