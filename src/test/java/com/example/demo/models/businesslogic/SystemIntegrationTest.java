package com.example.demo.models.businesslogic;

import com.example.demo.DemoApplication;
import com.example.demo.Service.DomainService;
import com.example.demo.Service.QuestionService;
import com.example.demo.models.businesslogic.backend.JenaBackend;
import com.example.demo.models.businesslogic.backend.PelletBackend;
import com.example.demo.models.businesslogic.domains.Domain;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.RoleInExercise;
import com.example.demo.models.repository.QuestionRepository;
import com.example.demo.models.repository.ResponseRepository;
import com.example.demo.utils.DomainAdapter;
import com.example.demo.utils.HyperText;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.demo.models.repository.ExerciseAttemptRepository;
import org.apache.commons.collections4.IterableUtils;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes= DemoApplication.class)
@Transactional
public class SystemIntegrationTest {
    @Autowired
    private Strategy strategy;

    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;

    @Autowired
    DomainService domainService;

//    @Autowired
//    PelletBackend backend;

    @Autowired
    JenaBackend backend;

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

        List<ExerciseAttemptEntity> testExerciseAttemptList = IterableUtils.toList( exerciseAttemptRepository.findAll());//Заполнить все значимые поля

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            List<Tag> tags = getTags(testExerciseAttemptList.get(0));
            assertEquals("a == b < c", question1.getQuestionText().getText());
            // double save
            questionService.saveQuestion(question1.questionData);
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(0));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
            assertEquals(1, mistakes.size());
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_higher_precedence_right",
                    mistakes.get(0).getLawName());
            List<HyperText> explanations  = explainMistakes(question3, mistakes);
            assertEquals(1, explanations.size());
            assertEquals("operator < on pos 4 should be evaluated before operator == on pos 2\n" +
                    " because operator < has higher precedence", explanations.get(0).getText());
        }
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            List<Tag> tags = getTags(testExerciseAttemptList.get(0));
            assertEquals("a == b < c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(0, 1));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
            assertEquals(1, mistakes.size());
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_higher_precedence_right",
                    mistakes.get(0).getLawName());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            List<Tag> tags = getTags(testExerciseAttemptList.get(0));
            assertEquals("a == b < c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(1));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
            assertTrue(mistakes.isEmpty());
        }
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            List<Tag> tags = getTags(testExerciseAttemptList.get(0));
            assertEquals("a == b < c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(1, 0));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
            assertTrue(mistakes.isEmpty());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(1));
            List<Tag> tags = getTags(testExerciseAttemptList.get(1));
            assertEquals("a == b < c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(0));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
            assertEquals(0, mistakes.size());
        }

        // Python question. Contrary to C++ "==" and "<" has same precedence
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(1));
            List<Tag> tags = getTags(testExerciseAttemptList.get(1));
            assertEquals("a == b < c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(1));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
            assertEquals(1, mistakes.size());
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left",
                    mistakes.get(0).getLawName());
            List<HyperText> explanations  = explainMistakes(question3, mistakes);
            assertEquals(1, explanations.size());
            assertEquals("operator == on pos 2 should be evaluated before operator < on pos 4\n" +
                    " because operator == has the same precedence and left associativity", explanations.get(0).getText());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(2));
            List<Tag> tags = getTags(testExerciseAttemptList.get(2));
            assertEquals("a + b + c * d", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(0));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
            assertTrue(mistakes.isEmpty());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(2));
            List<Tag> tags = getTags(testExerciseAttemptList.get(2));
            assertEquals("a + b + c * d", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(1));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
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
            Question question1 = generateQuestion(testExerciseAttemptList.get(3));
            List<Tag> tags = getTags(testExerciseAttemptList.get(3));
            assertEquals("a + b + c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(0));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
            assertTrue(mistakes.isEmpty());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(3));
            List<Tag> tags = getTags(testExerciseAttemptList.get(3));
            assertEquals("a + b + c", question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(1));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags);
            assertEquals(1, mistakes.size());
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left",
                    mistakes.get(0).getLawName());
        }
    }

    ResponseEntity makeResponse(AnswerObjectEntity answer) {
        ResponseEntity response = new ResponseEntity();
        response.setLeftAnswerObject(answer);
        response.setRightAnswerObject(answer);
        responseRepository.save(response);
        return response;
    }

    Question generateQuestion(ExerciseAttemptEntity exerciseAttempt) {
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

    Long solveQuestion(Question question, List<Tag> tags) {
        Domain domain = DomainAdapter.getDomain(question.questionData.getDomainEntity().getName());
        assertNotNull(domain);
        List<BackendFactEntity> solution = backend.solve(
                new ArrayList<>(domain.getQuestionPositiveLaws(question.getQuestionDomainType(), tags)),
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

    List<MistakeEntity> judgeQuestion(Question question, List<Tag> tags) {
        Domain domain = DomainAdapter.getDomain(question.questionData.getDomainEntity().getName());
        List<BackendFactEntity> responseFacts = question.responseToFacts();
        assertFalse(responseFacts.isEmpty());
        List<BackendFactEntity> violations = backend.judge(
                new ArrayList<>(domain.getQuestionNegativeLaws(question.getQuestionDomainType(), tags)),
                question.getStatementFacts(),
                question.getSolutionFacts(),
                responseFacts,
                domain.getViolationVerbs(question.getQuestionDomainType(), question.getStatementFacts())
        );
        return domain.interpretSentence(violations);
    }

    List<Tag> getTags(ExerciseAttemptEntity exerciseAttempt) {
        String[] tags = exerciseAttempt.getExercise().getTags().split(",");
        List<Tag> result = new ArrayList<>();
        for (String tagString : tags) {
            Tag tag = new Tag();
            tag.setName(tagString);
            result.add(tag);
        }
        for (String tagString : List.of("basics", "operators", "order", "evaluation")) {
            Tag tag = new Tag();
            tag.setName(tagString);
            result.add(tag);
        }
        return result;
    }

    List<HyperText> explainMistakes(Question question, List<MistakeEntity> mistakes) {
        Domain domain = DomainAdapter.getDomain(question.questionData.getDomainEntity().getName());
        return domain.makeExplanation(mistakes, FeedbackType.EXPLANATION);
    }

    private boolean checkQuestionRequest(QuestionRequest qr, ExerciseAttemptEntity testExerciseAttempt) {
        List<String> deniedConcepts = new ArrayList<>();
        List<String> targetConcepts = new ArrayList<>();

        //Выделить из упражнения целевые и запрещенные законы
        for (ExerciseConceptEntity ec : testExerciseAttempt.getExercise().getExerciseConcepts()) {

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