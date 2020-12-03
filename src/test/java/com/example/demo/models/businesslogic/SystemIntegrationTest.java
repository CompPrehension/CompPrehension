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
import com.example.demo.models.repository.ResponseRepository;
import com.example.demo.utils.DomainAdapter;
import io.swagger.models.auth.In;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.demo.models.repository.ExerciseAttemptRepository;
import org.apache.commons.collections4.IterableUtils;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
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

    @Autowired
    ResponseRepository responseRepository;

    @Test
    public void generateTest() throws Exception
    {
        assertNotNull(strategy);

        List<ExerciseAttempt> testExerciseAttemptList = IterableUtils.toList( exerciseAttemptRepository.findAll());//Заполнить все значимые поля

        //TODO: check why double save cause Unimplemented Exception
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            assertEquals("a + b * c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1);
            Question question3 = responseQuestion(question2, List.of(0));
            List<Mistake> mistakes = judgeQuestion(question3);
            assertEquals(1, mistakes.size());
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_higher_precedence_right",
                    mistakes.get(0).getLawName());
        }
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            assertEquals("a + b * c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1);
            Question question3 = responseQuestion(question2, List.of(0, 1));
            List<Mistake> mistakes = judgeQuestion(question3);
            assertEquals(1, mistakes.size());
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_higher_precedence_right",
                    mistakes.get(0).getLawName());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            assertEquals("a + b * c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1);
            Question question3 = responseQuestion(question2, List.of(1));
            List<Mistake> mistakes = judgeQuestion(question3);
            assertTrue(mistakes.isEmpty());
        }
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            assertEquals("a + b * c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1);
            Question question3 = responseQuestion(question2, List.of(1, 0));
            List<Mistake> mistakes = judgeQuestion(question3);
            assertTrue(mistakes.isEmpty());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(1));
            assertEquals("a + b + c * d", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1);
            Question question3 = responseQuestion(question2, List.of(0));
            List<Mistake> mistakes = judgeQuestion(question3);
            assertTrue(mistakes.isEmpty());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(1));
            assertEquals("a + b + c * d", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1);
            Question question3 = responseQuestion(question2, List.of(1));
            List<Mistake> mistakes = judgeQuestion(question3);
            assertEquals(2, mistakes.size());
            HashSet<String> expected = new HashSet<>();
            expected.add("error_single_token_binary_operator_has_unevaluated_higher_precedence_right");
            expected.add("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left");

            HashSet<String> real = new HashSet<>();
            real.add(mistakes.get(0).getLawName());
            real.add(mistakes.get(1).getLawName());
            assertEquals(expected, real);
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(2));
            assertEquals("a + b + c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1);
            Question question3 = responseQuestion(question2, List.of(0));
            List<Mistake> mistakes = judgeQuestion(question3);
            assertTrue(mistakes.isEmpty());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(2));
            assertEquals("a + b + c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1);
            Question question3 = responseQuestion(question2, List.of(1));
            List<Mistake> mistakes = judgeQuestion(question3);
            assertEquals(1, mistakes.size());
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left",
                    mistakes.get(0).getLawName());
        }
    }

    Response makeResponse(AnswerObject answer) {
        Response response = new Response();
        response.setLeftAnswerObject(answer);
        response.setRightAnswerObject(answer);
        responseRepository.save(response);
        return response;
    }

    Question generateQuestion(ExerciseAttempt exerciseAttempt) {
        assertNotNull(exerciseAttempt);
        Domain domain = DomainAdapter.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        assertNotNull(exerciseAttempt.getExercise());
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        assertTrue(checkQuestionRequest(qr, exerciseAttempt));
        Question question = domain.makeQuestion(qr, exerciseAttempt.getUser().getPreferred_language());
        assertNotNull(question);
        //TODO: domain set domainEntity into question
        question.questionData.setDomainEntity(domainService.getDomainEntity(domain.getName()));
        return question;
    }

    Question getQuestion(Long questionId) {
        Question question = questionService.generateBusinessLogicQuestion(questionRepository.findById(questionId).get());
        assertNotNull(question);
        assertNotNull(question.questionData.getId());
        return question;
    }

    Long solveQuestion(Question question) {
        Domain domain = DomainAdapter.getDomain(question.questionData.getDomainEntity().getName());
        assertNotNull(domain);
        List<BackendFact> solution = backend.solve(
                domain.getQuestionLaws(question.getQuestionDomainType(), question.getStatementFacts()),
                question.getStatementFacts(),
                domain.getSolutionVerbs(question.getQuestionDomainType(), question.getStatementFacts()));
        assertFalse(solution.isEmpty());
        question.questionData.setSolutionFacts(solution);
        questionService.saveQuestion(question.questionData);
        return question.questionData.getId();
    }

    Question responseQuestion(Long questionId, List<Integer> responses) {
        Question question = getQuestion(questionId);
        for (Integer response : responses) {
            assertTrue(response < question.getAnswerObjects().size());
            question.addResponse(makeResponse(question.getAnswerObject(response)));
        }
        return question;
    }

    List<Mistake> judgeQuestion(Question question) {
        Domain domain = DomainAdapter.getDomain(question.questionData.getDomainEntity().getName());
        List<BackendFact> responseFacts = question.responseToFacts();
        assertFalse(responseFacts.isEmpty());
        List<BackendFact> violations = backend.judge(
                new ArrayList<>(domain.getQuestionNegativeLaws(question.getQuestionDomainType(), question.getStatementFacts())),
                question.getStatementFacts(),
                question.getSolutionFacts(),
                responseFacts,
                domain.getViolationVerbs(question.getQuestionDomainType(), question.getStatementFacts())
        );
        return domain.interpretSentence(violations);
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