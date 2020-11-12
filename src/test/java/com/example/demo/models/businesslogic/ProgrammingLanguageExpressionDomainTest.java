package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.EnumData.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

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
    public void testQuestionGeneration() throws Exception {
        List<Concept> concepts = new ArrayList<>();
        concepts.add(domain.getConcept("precedence"));
        QuestionRequest qr = new QuestionRequest();
        qr.setTargetConcepts(concepts);
        assertEquals(domain.makeQuestion(qr, Language.ENGLISH).getQuestionText().getText(), "a + b * c");

        concepts.add(domain.getConcept("associativity"));
        QuestionRequest qr2 = new QuestionRequest();
        qr2.setTargetConcepts(concepts);
        assertEquals(domain.makeQuestion(qr2, Language.ENGLISH).getQuestionText().getText(), "* * b");
    }
}