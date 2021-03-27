package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.CompPrehensionApplication;
import org.vstu.compprehension.Service.DomainService;
import org.vstu.compprehension.Service.InteractionService;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.InteractionType;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import org.vstu.compprehension.models.repository.QuestionRepository;
import org.vstu.compprehension.models.repository.ResponseRepository;
import org.vstu.compprehension.utils.DomainAdapter;
import org.vstu.compprehension.utils.HyperText;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.apache.commons.collections4.IterableUtils;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes= CompPrehensionApplication.class)
@Transactional
public class SystemIntegrationTest {
    @Autowired
    private Strategy strategy;

    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;

    @Autowired
    DomainService domainService;

    @Autowired
    InteractionService interactionService;

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
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            // double save
            questionService.saveQuestion(question1.questionData);
            Long question2 = solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2);
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            Domain.CorrectAnswer correctAnswer = getCorrectAnswer(question2);
            assertEquals("operator_binary_<", correctAnswer.answer.getConcept());
            assertEquals("single_token_binary_execution", correctAnswer.lawName);
            Question question3 = responseQuestion(question2, List.of(0));
            Domain.InterpretSentenceResult result = judgeQuestion(question3, tags);

            //Сохранение интеракции
            InteractionEntity ie = new InteractionEntity();
            ie.setQuestion(question1.questionData);
            ie.setInteractionType(InteractionType.SEND_RESPONSE);//Какой нужен?
            ie.setMistakes(result.mistakes);
            ie.setOrderNumber(result.IterationsLeft);//Показатель порядка?
            //ie.setFeedback();Где взять?
            ArrayList<CorrectLawEntity> cles = new ArrayList<>();
            for(int i = 0; i < result.correctlyAppliedLaws.size(); i++){
                CorrectLawEntity cle = new CorrectLawEntity();
                cle.setLawName(result.correctlyAppliedLaws.get(i));
                cle.setInteraction(ie);
                cles.add(cle);
            }
            ie.setCorrectLaw(cles);
            interactionService.saveInteraction(ie);
            ie = interactionService.getInteraction(ie.getId()).get();
            ArrayList<InteractionEntity> ies = new ArrayList<>();
            if(question1.questionData.getInteractions() != null) {
                ies.addAll(question1.questionData.getInteractions());
            }
            ies.add(ie);
            question1.questionData.setInteractions(ies);
            questionService.saveQuestion(question1.questionData);

            //interactionService.saveInteraction(ie);
            Question q = questionService.getQuestion(question2);
            assertEquals(1, result.mistakes.size());
            assertEquals(0, result.correctlyAppliedLaws.size());
            assertEquals(1, result.CountCorrectOptions);
            assertEquals(2, result.IterationsLeft);
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_higher_precedence_right",
                    result.mistakes.get(0).getLawName());
            List<HyperText> explanations  = explainMistakes(question3, result.mistakes);
            assertEquals(1, explanations.size());
            assertEquals("operator < on pos 4 should be evaluated before operator == on pos 2\n" +
                    " because operator < has higher precedence", explanations.get(0).getText());
            Long supQ = generateSupplementaryQuestion(result, testExerciseAttemptList.get(0));
            Question supQ2 = questionService.getQuestion(supQ);
            assertEquals(QuestionType.MULTI_CHOICE, supQ2.getQuestionType());
            assertEquals("OrderOperatorsSupplementary", supQ2.getQuestionDomainType());

            float grade = strategy.grade(IterableUtils.toList(exerciseAttemptRepository.findAll()).get(0));
        }
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            List<Tag> tags = getTags(testExerciseAttemptList.get(0));
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2);
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            Question question3 = responseQuestion(question2, List.of(0, 1));
            Domain.InterpretSentenceResult result = judgeQuestion(question3, tags);
            assertEquals(1, result.mistakes.size());
            assertEquals(0, result.correctlyAppliedLaws.size());
            assertEquals(0, result.CountCorrectOptions);
            assertEquals(1, result.IterationsLeft);
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_higher_precedence_right",
                    result.mistakes.get(0).getLawName());
            float grade = strategy.grade(testExerciseAttemptList.get(0));
        }
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            List<Tag> tags = getTags(testExerciseAttemptList.get(0));
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            questionService.saveQuestion(question1.questionData);
            Long question2 = solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2);
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            Question question3 = responseQuestion(question2, List.of(1));
            Domain.InterpretSentenceResult result = judgeQuestion(question3, tags);
            assertEquals(0, result.mistakes.size());
            assertEquals(1, result.correctlyAppliedLaws.size());
            assertEquals("error_single_token_binary_operator_has_unevaluated_higher_precedence_right", result.correctlyAppliedLaws.get(0));
            assertEquals(1, result.CountCorrectOptions);
            assertEquals(1, result.IterationsLeft);

            //Сохранение интеракции
            InteractionEntity ie = new InteractionEntity();
            ie.setQuestion(question1.questionData);
            ie.setInteractionType(InteractionType.SEND_RESPONSE);//Какой нужен?
            ie.setMistakes(result.mistakes);
            ie.setOrderNumber(result.IterationsLeft);//Показатель порядка?
            //ie.setFeedback();Где взять?
            ArrayList<CorrectLawEntity> cles = new ArrayList<>();
            for(int i = 0; i < result.correctlyAppliedLaws.size(); i++){
                CorrectLawEntity cle = new CorrectLawEntity();
                cle.setLawName(result.correctlyAppliedLaws.get(i));
                cle.setInteraction(ie);
                cles.add(cle);
            }
            ie.setCorrectLaw(cles);
            interactionService.saveInteraction(ie);
            ie = interactionService.getInteraction(ie.getId()).get();
            ArrayList<InteractionEntity> ies = new ArrayList<>();
            if(question1.questionData.getInteractions() != null) {
                ies.addAll(question1.questionData.getInteractions());
            }
            ies.add(ie);
            question1.questionData.setInteractions(ies);
            questionService.saveQuestion(question1.questionData);

            float grade = strategy.grade(testExerciseAttemptList.get(0));
        }
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(0));
            List<Tag> tags = getTags(testExerciseAttemptList.get(0));
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2);
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            Question question3 = responseQuestion(question2, List.of(1, 0));
            Domain.InterpretSentenceResult result = judgeQuestion(question3, tags);
            assertEquals(0, result.mistakes.size());
            assertEquals(0, result.correctlyAppliedLaws.size());
            assertEquals(0, result.CountCorrectOptions);
            assertEquals(0, result.IterationsLeft);

            float grade = strategy.grade(testExerciseAttemptList.get(0));
        }

        // Python question. Contrary to C++ "==" and "<" has same precedence
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(1));
            List<Tag> tags = getTags(testExerciseAttemptList.get(1));
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2);
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            Question question3 = responseQuestion(question2, List.of(0));
            Domain.InterpretSentenceResult result = judgeQuestion(question3, tags);
            assertEquals(0, result.mistakes.size());
            assertEquals(1, result.correctlyAppliedLaws.size());
            assertEquals("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left", result.correctlyAppliedLaws.get(0));
            assertEquals(1, result.CountCorrectOptions);
            assertEquals(1, result.IterationsLeft);

        }

        // Python question. Contrary to C++ "==" and "<" has same precedence
        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(1));
            List<Tag> tags = getTags(testExerciseAttemptList.get(1));
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2);
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            Question question3 = responseQuestion(question2, List.of(1));
            Domain.InterpretSentenceResult result = judgeQuestion(question3, tags);
            assertEquals(1, result.mistakes.size());
            assertEquals(0, result.correctlyAppliedLaws.size());
            assertEquals(1, result.CountCorrectOptions);
            assertEquals(2, result.IterationsLeft);

            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left",
                    result.mistakes.get(0).getLawName());
            List<HyperText> explanations  = explainMistakes(question3, result.mistakes);
            assertEquals(1, explanations.size());
            assertEquals("operator == on pos 2 should be evaluated before operator < on pos 4\n" +
                    " because operator == has the same precedence and left associativity", explanations.get(0).getText());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(2));
            List<Tag> tags = getTags(testExerciseAttemptList.get(2));
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a + b + c * d"), question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(0));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags).mistakes;
            assertTrue(mistakes.isEmpty());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(2));
            List<Tag> tags = getTags(testExerciseAttemptList.get(2));
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a + b + c * d"), question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(1));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags).mistakes;
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
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a + b + c"), question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(0));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags).mistakes;
            assertTrue(mistakes.isEmpty());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(3));
            List<Tag> tags = getTags(testExerciseAttemptList.get(3));
            assertEquals(ProgrammingLanguageExpressionDomain.ExpressionToHtml("a + b + c"), question1.getQuestionText().getText());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = responseQuestion(question2, List.of(1));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags).mistakes;
            assertEquals(1, mistakes.size());
            assertEquals(
                    "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left",
                    mistakes.get(0).getLawName());
        }

        {
            Question question1 = generateQuestion(testExerciseAttemptList.get(4));
            Tag tag = new Tag();
            tag.setName("type");
            List<Tag> tags = new ArrayList<>(Arrays.asList(tag));
            assertEquals("MATCHING", question1.getQuestionType().name());
            Long question2 = solveQuestion(question1, tags);
            Question question3 = getQuestion(question2);
            question3.addResponse(makeResponse(question3.getAnswerObject(2), question3.getAnswerObject(8)));
            List<MistakeEntity> mistakes = judgeQuestion(question3, tags).mistakes;
            assertEquals(0, mistakes.size());
        }
    }

    ResponseEntity makeResponse(AnswerObjectEntity answer) {
        return makeResponse(answer, answer);
    }

    ResponseEntity makeResponse(AnswerObjectEntity answerL, AnswerObjectEntity answerR) {
        ResponseEntity response = new ResponseEntity();
        response.setLeftAnswerObject(answerL);
        response.setRightAnswerObject(answerR);
        responseRepository.save(response);
        return response;
    }

    Question generateQuestion(ExerciseAttemptEntity exerciseAttempt) {
        assertNotNull(exerciseAttempt);
        Domain domain = DomainAdapter.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        assertNotNull(exerciseAttempt.getExercise());
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        assertTrue(checkQuestionRequest(qr, exerciseAttempt));
        Question question = domain.makeQuestion(qr, getTags(exerciseAttempt), exerciseAttempt.getUser().getPreferred_language());
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

    Domain.ProcessSolutionResult getSolveInfo(Long questionId) {
        Question question = getQuestion(questionId);
        Domain domain = DomainAdapter.getDomain(question.questionData.getDomainEntity().getName());
        return domain.processSolution(question.getSolutionFacts());
    }

    Domain.CorrectAnswer getCorrectAnswer(Long questionId) {
        Question question = getQuestion(questionId);
        Domain domain = DomainAdapter.getDomain(question.questionData.getDomainEntity().getName());
        return domain.getAnyNextCorrectAnswer(question);
    }

    Domain.InterpretSentenceResult judgeQuestion(Question question, List<Tag> tags) {
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

    Long generateSupplementaryQuestion(Domain.InterpretSentenceResult interpretSentenceResult, ExerciseAttemptEntity exerciseAttempt) {
        Domain domain = DomainAdapter.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        Question question = domain.makeSupplementaryQuestion(interpretSentenceResult, exerciseAttempt);
        assertNotNull(question);
        question.questionData.setDomainEntity(domainService.getDomainEntity(domain.getName()));
        questionService.saveQuestion(question.questionData);
        return question.questionData.getId();
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