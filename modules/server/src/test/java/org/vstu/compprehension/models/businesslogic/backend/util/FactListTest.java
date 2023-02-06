package org.vstu.compprehension.models.businesslogic.backend.util;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.repository.DomainRepository;

import java.util.List;
import java.util.Optional;

import static org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain.QUESTIONS_CONFIG_PATH;

//@SpringBootTest
public class FactListTest {
    private static List<Question> QUESTIONS = null;
    FactList fl;

    @Autowired
    ControlFlowStatementsDomain domain;


    @BeforeAll
    public static void setUpFirst() throws Exception {
//        fl = new FactList();
        ControlFlowStatementsDomain.initVocab();
        QUESTIONS = ControlFlowStatementsDomain.readQuestions(FactListTest.class.getClassLoader().getResourceAsStream(QUESTIONS_CONFIG_PATH));
    }

    @BeforeEach
    public void setUp() throws Exception {
//        fl = new FactList();
//        domain = new DomainFactory().getDomain();
    }

    @Test
    public void test_fromModel() {
        Model schemaModel = ControlFlowStatementsDomain.getVocabulary().getModel();
        fl = new FactList(schemaModel);
        System.out.println(fl.size());
        for (BackendFactEntity fact : fl) {
//            ((FactTriple)fact).updateFactsFromStatement();
            System.out.println(fact);
        }
    }

    @Test
    public void test_fromFacts() {
//        Model schemaModel = ControlFlowStatementsDomain.getVocabulary().getModel();
        Question q = QUESTIONS.get(0);
        fl = new FactList(q.getStatementFacts());
        System.out.println(fl.size());
        for (BackendFactEntity fact : fl) {
//            ((FactTriple)fact).updateFactsFromStatement();
            System.out.println(fact);
        }
    }

    @Test
    public void test_add() {
        Model schemaModel = ControlFlowStatementsDomain.getVocabulary().getModel();
        fl = new FactList(schemaModel);
        System.out.println(fl.size());

        Question q = QUESTIONS.get(0);
        FactList fl2 = new FactList(q.getStatementFacts());
        System.out.println(fl2.size());

        fl.addAll(fl2);

        System.out.println(fl.size());
        for (BackendFactEntity fact : fl) {
//            ((FactTriple)fact).updateFactsFromStatement();
//            String factStr = fact.toString();
//            System.out.println(factStr);
            /*if (factStr.contains("http:"))*/ {
                System.out.println(((FactTriple) fact).getStatement());
            }
        }
    }

    @Test
    public void test_addFacts() {
        Model schemaModel = ControlFlowStatementsDomain.getVocabulary().getModel();
        fl = new FactList(schemaModel);
        System.out.println(fl.size());

        Question q = QUESTIONS.get(0);
        fl.addAll(q.getStatementFacts());

        System.out.println(fl.size());
        for (BackendFactEntity fact : fl) {
            String factStr = fact.toString();
            System.out.println(factStr);
        }
    }

}
