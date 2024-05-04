package org.vstu.compprehension.models.businesslogic.backend.util;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;

import java.util.List;

import static org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain.QUESTIONS_CONFIG_PATH;

//@SpringBootTest
public class JenaFactListTest {
    private static List<Question> QUESTIONS = null;
    JenaFactList fl;

    @BeforeAll
    public static void setUpFirst() {
//        fl = new FactList();
        ControlFlowStatementsDomain.initVocab();
        var domain = new ControlFlowStatementsDomain(null, null, null, null);
        QUESTIONS = domain.readQuestions(JenaFactListTest.class.getClassLoader().getResourceAsStream(QUESTIONS_CONFIG_PATH));
    }

    @BeforeEach
    public void setUp() {
//        fl = new FactList();
//        domain = new DomainFactory().getDomain();
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
//        Model schemaModel = ControlFlowStatementsDomain.getVocabulary().getModel();
        Question q = QUESTIONS.get(0);
        fl = new JenaFactList();
        fl.addBackendFacts(q.getStatementFacts());
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

        Question q = QUESTIONS.get(0);
        JenaFactList fl2 = JenaFactList.fromBackendFacts(q.getStatementFacts());
        System.out.println(fl2.size());

        fl.addAll(fl2);

        System.out.println(fl.size());
        for (Fact fact : fl) {
//            ((FactTriple)fact).updateFactsFromStatement();
//            String factStr = fact.toString();
//            System.out.println(factStr);
            /*if (factStr.contains("http:"))*/ {
                System.out.println(((JenaFact) fact).getStatement());
            }
        }
    }

    @Test
    public void test_addFacts() {
        Model schemaModel = ControlFlowStatementsDomain.getVocabulary().getModel();
        fl = new JenaFactList(schemaModel);
        System.out.println(fl.size());

        Question q = QUESTIONS.get(0);
        fl.addBackendFacts(q.getStatementFacts());

        System.out.println(fl.size());
        for (Fact fact : fl) {
            String factStr = fact.toString();
            System.out.println(factStr);
        }
    }

}
