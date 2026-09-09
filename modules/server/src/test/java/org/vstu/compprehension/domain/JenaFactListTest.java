package org.vstu.compprehension.domain;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.data.question.GeneratedQuestionData;
import org.vstu.compprehension.businesslogic.backend.Fact;
import org.vstu.compprehension.businesslogic.backend.facts.JenaFact;
import org.vstu.compprehension.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.businesslogic.domains.ControlFlowStatementsDomain;

import java.util.List;

import static org.vstu.compprehension.businesslogic.domains.ControlFlowStatementsDomain.QUESTIONS_CONFIG_PATH;

public class JenaFactListTest {
    private static List<GeneratedQuestionData> QUESTIONS = null;
    JenaFactList fl;

    @BeforeAll
    public static void setUpFirst() {
        ControlFlowStatementsDomain.initVocab();
        var domain = new ControlFlowStatementsDomain(new DomainData("ControlFlowStatementsDomain", "ControlFlowStatementsDomain", "1.0.0", null), null, null, null);
        QUESTIONS = domain.readQuestions(JenaFactListTest.class.getClassLoader().getResourceAsStream(QUESTIONS_CONFIG_PATH));
    }

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void test_fromModel() {
        Model schemaModel = ControlFlowStatementsDomain.getVocabulary().getModel();
        fl = new JenaFactList(schemaModel);
        System.out.println(fl.size());
        for (Fact fact : fl) {
            ((JenaFact)fact).updateFactFromStatement();
            System.out.println(fact);
        }
    }

    @Test
    public void test_fromFacts() {
        GeneratedQuestionData q = QUESTIONS.get(0);
        fl = new JenaFactList();
        fl.addBackendFacts(q.getContent().getStatementFacts());
        System.out.println(fl.size());
        for (Fact fact : fl) {
            ((JenaFact)fact).updateFactFromStatement();
            System.out.println(fact);
        }
    }

    @Test
    public void test_add() {
        Model schemaModel = ControlFlowStatementsDomain.getVocabulary().getModel();
        fl = new JenaFactList(schemaModel);
        System.out.println(fl.size());

        GeneratedQuestionData q = QUESTIONS.get(0);
        JenaFactList fl2 = JenaFactList.fromBackendFacts(q.getContent().getStatementFacts());
        System.out.println(fl2.size());

        fl.addAll(fl2);

        System.out.println(fl.size());
        for (Fact fact : fl) {
            System.out.println(((JenaFact) fact).getStatement());
        }
    }

    @Test
    public void test_addFacts() {
        Model schemaModel = ControlFlowStatementsDomain.getVocabulary().getModel();
        fl = new JenaFactList(schemaModel);
        System.out.println(fl.size());

        GeneratedQuestionData q = QUESTIONS.get(0);
        fl.addBackendFacts(q.getContent().getStatementFacts());

        System.out.println(fl.size());
        for (Fact fact : fl) {
            String factStr = fact.toString();
            System.out.println(factStr);
        }
    }

}
