package com.example.demo.models.businesslogic;

import com.example.demo.DemoApplication;
import com.example.demo.Service.DomainService;
import com.example.demo.Service.QuestionService;
import com.example.demo.models.businesslogic.backend.Backend;
import com.example.demo.models.businesslogic.backend.PelletBackend;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.RoleInExercise;
import com.example.demo.models.repository.QuestionRepository;
import com.example.demo.utils.DomainAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.demo.models.repository.ExerciseAttemptRepository;
import org.apache.commons.collections4.IterableUtils;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes= DemoApplication.class)
@Transactional
public class SystemIntegrationTest {
    @Autowired
    private Strategy strategy;

    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;

    @Autowired
    DomainService domainService;

    @Autowired
    PelletBackend backend;

    @Autowired
    QuestionService questionService;

    @Autowired
    QuestionRepository questionRepository;

    @Test
    public void generateTest() throws Exception
    {
        assertNotNull(strategy);

        List<ExerciseAttempt> testExerciseAttemptList = IterableUtils.toList( exerciseAttemptRepository.findAll());//Заполнить все значимые поля

        Domain domain = DomainAdapter.getDomain(testExerciseAttemptList.get(0).getExercise().getDomain().getName());

        ExerciseAttempt testExerciseAttempt = testExerciseAttemptList.get(0);

        assertNotNull(testExerciseAttempt.getExercise());

        QuestionRequest qr = strategy.generateQuestionRequest(testExerciseAttempt);

        assertTrue(checkQuestionRequest(qr, testExerciseAttempt));

        Question question = domain.makeQuestion(qr, Language.ENGLISH);
        //TODO: domain set domainEntity into question
        question.questionData.setDomainEntity(domainService.getDomainEntity(domain.getName()));
        assertEquals("a + b * c", question.getQuestionText().getText());
        questionService.saveQuestion(question.questionData);
        Long questionId = question.questionData.getId();

        Question question1 = questionService.generateBusinessLogicQuestion(questionRepository.findById(questionId).get());
        List<BackendFact> solution = backend.solve(
                domain.getQuestionLaws(question1.getQuestionDomainType(), question1.getStatementFacts()),
                question.getStatementFacts(),
                domain.getSolutionVerbs(question1.getQuestionDomainType(), question1.getStatementFacts()));
        assertFalse(solution.isEmpty());
        question1.questionData.setSolutionFacts(solution);

        question1.addResponse(makeResponse(question1.getAnswerObject(0)));
        List<BackendFact> responseFacts = question1.responseToFacts();
        assertFalse(responseFacts.isEmpty());
        List<BackendFact> violations = backend.judge(
                domain.getQuestionLaws(question1.getQuestionDomainType(), question1.getStatementFacts()),
                question1.getStatementFacts(),
                solution,
                responseFacts,
                domain.getViolationVerbs(question1.getQuestionDomainType(), question1.getStatementFacts())
        );
        assertFalse(violations.isEmpty());
        List<Mistake> mistakes = domain.interpretSentence(violations);
        assertFalse(mistakes.isEmpty());
        assertEquals(1, mistakes.size());
        assertEquals(
                "error_single_token_binary_operator_has_unevaluated_higher_precedence_right",
                mistakes.get(0).getLawName());

        testExerciseAttempt = testExerciseAttemptList.get(1);
        QuestionRequest qr1 = strategy.generateQuestionRequest(testExerciseAttempt);

        assertTrue(checkQuestionRequest(qr1, testExerciseAttempt));

        Question q1 = domain.makeQuestion(qr1, Language.ENGLISH);
        assertFalse(q1.getQuestionText().getText().isEmpty());
        //Вызов домена с проверкой адекватности вопросов и тд

        testExerciseAttempt = testExerciseAttemptList.get(2);
        QuestionRequest qr2 = strategy.generateQuestionRequest(testExerciseAttempt);

        assertTrue(checkQuestionRequest(qr2, testExerciseAttempt));


        Question q2 = domain.makeQuestion(qr2, Language.ENGLISH);
        assertFalse(q2.getQuestionText().getText().isEmpty());
        //Вызов домена с проверкой адекватности вопросов и тд

    }

    Response makeResponse(AnswerObject answer) {
        Response response = new Response();
        response.setLeftAnswerObject(answer);
        response.setRightAnswerObject(answer);
        return response;
    }

    void checkQuestionSolve(Question question, List<BackendFact> statementsFacts) {
        return;
    }

    void checkQuestionJudge(Question question, List<BackendFact> violationFacts) {
        return;
    }

    private boolean checkQuestionRequest(QuestionRequest qr, ExerciseAttempt testExerciseAttempt) {
        List<String> deniedConcepts = new ArrayList<>();
        List<String> targetConcepts = new ArrayList<>();

        //Выделить из упражнения целевые и запрещенные законы
        for (ExerciseConcept ec : testExerciseAttempt.getExercise().getExerciseConcepts()) {

            if (ec.getRoleInExercise() == RoleInExercise.TARGETED) {

                targetConcepts.add(ec.getConceptName());

            } else if (ec.getRoleInExercise() == RoleInExercise.FORBIDDEN) {

                deniedConcepts.add(ec.getConceptName());

            }
        }

        //Все целевые концепты должны быть внесены (а если лишние, но не противоречащие запрещённым?)
        if (!qr.getTargetConcepts().stream().map(i -> i.getName()).collect(Collectors.toList())
                .containsAll(targetConcepts)) {
            return false;
        }

        //все запрещённые концепты должны быть обозначены
        if (!deniedConcepts.containsAll(qr.getDeniedConcepts().stream().map(i -> i.getName()).collect(Collectors.toList()))) {
            return false;
        }

        for (String deniedConcept : deniedConcepts){
            //Среди целевых концептов запроса не должно быть запрещённых концептов задания
            if(qr.getTargetConcepts().stream().map(i -> i.getName()).collect(Collectors.toList()).contains(deniedConcept)){
                return false;
            }
        }

        return true;
    }
}