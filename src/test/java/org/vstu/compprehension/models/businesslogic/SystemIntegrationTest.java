package org.vstu.compprehension.models.businesslogic;

import lombok.val;
import org.vstu.compprehension.CompPrehensionApplication;
import org.vstu.compprehension.Service.DomainService;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.InteractionType;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.utils.DomainAdapter;
import org.vstu.compprehension.utils.HyperText;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    private InteractionRepository interactionRepository;

    @Autowired
    JenaBackend backend;

    @Autowired
    QuestionService questionService;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    ResponseRepository responseRepository;

    @Autowired
    FeedbackRepository feedbackRepository;

    @Test
    public void generateTest() throws Exception
    {

        assertNotNull(strategy);
/*
        val testExerciseAttemptList = IterableUtils
                .toList(exerciseAttemptRepository.findAll())
                .stream()
                .collect(Collectors.toMap(v -> v.getId().intValue(), v -> v, (prev, next) -> next, HashMap::new));
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(4);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(4).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            // double save
            questionService.saveQuestion(question1.questionData);
            val question2 = questionService.solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2.getQuestionData().getId());
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            Domain.CorrectAnswer correctAnswer = questionService.getNextCorrectAnswer(question2);
            assertEquals("operator_binary_<", correctAnswer.answers.get(correctAnswer.answers.size() - 1).getLeft().getConcept());
            assertEquals("single_token_binary_execution", correctAnswer.lawName);
            assertEquals("Operator < at pos 4 evaluates \n" +
                    "before operator == at pos 2: operator < has higher precedence than operator ==\n", correctAnswer.explanation.toString());
            val question2Responses = questionService.responseQuestion(question2, List.of(0));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question2, question2Responses, tags);

            //Сохранение интеракции
            InteractionEntity ie = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question1.questionData,
                    result.violations,
                    result.correctlyAppliedLaws,
                    question2Responses
            );
            ArrayList<InteractionEntity> ies = new ArrayList<>();
            if(question1.questionData.getInteractions() != null) {
                ies.addAll(question1.questionData.getInteractions());
            }
            ies.add(ie);
            question1.questionData.setInteractions(ies);
            questionService.saveQuestion(question1.questionData);

            //interactionService.saveInteraction(ie);
            Question q = questionService.getQuestion(question2.getQuestionData().getId());
            assertEquals(1, result.violations.size());
            assertEquals(0, result.correctlyAppliedLaws.size());
            assertEquals(1, result.CountCorrectOptions);
            assertEquals(2, result.IterationsLeft);
            assertEquals(
                    "error_base_higher_precedence_right",
                    result.violations.get(0).getLawName());
            List<HyperText> explanations  = questionService.explainViolations(question2, result.violations);
            assertEquals(1, explanations.size());
            assertEquals("operator < at pos 4 should be evaluated before operator == at pos 2\n" +
                    " because operator < has higher precedence", explanations.get(0).getText());
            Long supQ = questionService.generateSupplementaryQuestion(question2.getQuestionData(), result.violations.get(0)).getQuestionData().getId();
            Question supQ2 = questionService.getQuestion(supQ);
            assertEquals(QuestionType.SINGLE_CHOICE, supQ2.getQuestionType());
            assertEquals("OrderOperatorsSupplementary", supQ2.getQuestionDomainType());
            val question2Responses2 = questionService.responseQuestion(supQ2, List.of(3));
            Domain.InterpretSentenceResult resSup = questionService.judgeSupplementaryQuestion(supQ2, question2Responses2, testExerciseAttemptList.get(4));
            assertEquals("select_highest_precedence_right_operator", resSup.violations.get(0).getLawName());
            assertNull(resSup.violations.get(0).getDetailedLawName());
            assertTrue(resSup.isAnswerCorrect);

            Long supQ3 = questionService.generateSupplementaryQuestion(question2.getQuestionData(), resSup.violations.get(0)).getQuestionData().getId();
            Question supQ4 = questionService.getQuestion(supQ3);
            val question2Responses3 = questionService.responseQuestion(supQ4, List.of(1));
            Domain.InterpretSentenceResult resSup2 = questionService.judgeSupplementaryQuestion(supQ4, question2Responses3, testExerciseAttemptList.get(4));
            assertEquals("error_select_highest_precedence_operator_right", resSup2.violations.get(0).getLawName());
            assertEquals("error_select_highest_precedence", resSup2.violations.get(0).getDetailedLawName());
            assertFalse(resSup2.isAnswerCorrect);

            Long supQ31 = questionService.generateSupplementaryQuestion(question2.getQuestionData(), resSup2.violations.get(0)).getQuestionData().getId();
            Question supQ41 = questionService.getQuestion(supQ31);
            assertTrue(supQ41.getAnswerObjects().isEmpty());
            assertTrue(supQ41.getQuestionText().toString().startsWith("<div class='comp-ph-question'>Wrong, precedence of operator < is higher than operator =="));

            Long supQ5 = questionService.generateSupplementaryQuestion(question2.getQuestionData(), result.violations.get(0)).getQuestionData().getId();
            Question supQ6 = questionService.getQuestion(supQ5);
            val question2Responses4 = questionService.responseQuestion(supQ6, List.of(4));
            Domain.InterpretSentenceResult resSup3 = questionService.judgeSupplementaryQuestion(supQ6, question2Responses4, testExerciseAttemptList.get(4));
            assertEquals("select_precedence_or_associativity_right_influence", resSup3.violations.get(0).getLawName());
            assertNull(resSup3.violations.get(0).getDetailedLawName());
            assertFalse(resSup3.isAnswerCorrect);

            Long supQ7 = questionService.generateSupplementaryQuestion(question2.getQuestionData(), resSup3.violations.get(0)).getQuestionData().getId();
            Question supQ8 = questionService.getQuestion(supQ7);
            val question2Responses5 = questionService.responseQuestion(supQ8, List.of(0));
            Domain.InterpretSentenceResult resSup4 = questionService.judgeSupplementaryQuestion(supQ8, question2Responses5, testExerciseAttemptList.get(4));
            assertEquals("select_highest_precedence_right_operator", resSup4.violations.get(0).getLawName());
            assertNull(resSup4.violations.get(0).getDetailedLawName());
            assertTrue(resSup4.isAnswerCorrect);

            Long supQ9 = questionService.generateSupplementaryQuestion(question2.getQuestionData(), resSup4.violations.get(0)).getQuestionData().getId();
            Question supQ10 = questionService.getQuestion(supQ9);
            val question2Responses6 = questionService.responseQuestion(supQ10, List.of(0));
            Domain.InterpretSentenceResult resSup5 = questionService.judgeSupplementaryQuestion(supQ10, question2Responses6, testExerciseAttemptList.get(4));
            assertEquals("correct_select_highest_precedence_operator_right", resSup5.violations.get(0).getLawName());
            assertNull(resSup5.violations.get(0).getDetailedLawName());
            assertTrue(resSup5.isAnswerCorrect);
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(4);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(4).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            // double save
            questionService.saveQuestion(question1.questionData);
            Question question2 = questionService.solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2.getQuestionData().getId());
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            Domain.CorrectAnswer correctAnswer = questionService.getNextCorrectAnswer(question2);
            assertEquals("operator_binary_<", correctAnswer.answers.get(correctAnswer.answers.size() - 1).getLeft().getConcept());
            assertEquals("single_token_binary_execution", correctAnswer.lawName);
            assertEquals("Operator < at pos 4 evaluates \n" +
                    "before operator == at pos 2: operator < has higher precedence than operator ==\n", correctAnswer.explanation.toString());
            Question question3 = getQuestion(question2.getQuestionData().getId());

            val responses = new ArrayList<ResponseEntity>();
            responses.add(makeResponse(correctAnswer.answers.get(correctAnswer.answers.size() - 1).getLeft()));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question3, responses, tags);
            //Сохранение интеракции
            InteractionEntity ie = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question3.questionData,
                    result.violations,
                    result.correctlyAppliedLaws,
                    responses
            );
            ArrayList<InteractionEntity> ies = new ArrayList<>();
            if(question3.questionData.getInteractions() != null) {
                ies.addAll(question3.questionData.getInteractions());
            }
            ies.add(ie);
            question3.questionData.setInteractions(ies);
            assertTrue(result.violations.isEmpty());
            assertEquals("error_base_higher_precedence_right", result.correctlyAppliedLaws.get(0));
            questionService.saveQuestion(question3.questionData);

            Long question4 = question3.getQuestionData().getId();
            Domain.CorrectAnswer correctAnswer2 = questionService.getNextCorrectAnswer(question3);
            assertEquals("operator_binary_<", correctAnswer2.answers.get(correctAnswer.answers.size() - 1).getLeft().getConcept());
            assertEquals("single_token_binary_execution", correctAnswer2.lawName);
            assertEquals("Operator == at pos 2 evaluates \n" +
                    "after operator < at pos 4: operator == has lower precedence than operator <\n", correctAnswer2.explanation.toString());
            Question question5 = getQuestion(question4);
            responses.add(makeResponse(correctAnswer2.answers.get(correctAnswer.answers.size() - 1).getLeft()));
            Domain.InterpretSentenceResult result2 = questionService.judgeQuestion(question5, responses, tags);
            //Сохранение интеракции
            InteractionEntity ie2 = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question5.questionData,
                    result.violations,
                    result.correctlyAppliedLaws,
                    responses
            );
            ArrayList<InteractionEntity> ies2 = new ArrayList<>();
            if(question5.questionData.getInteractions() != null) {
                ies2.addAll(question5.questionData.getInteractions());
            }
            ies2.add(ie2);
            question5.questionData.setInteractions(ies2);
            questionService.saveQuestion(question5.questionData);

            assertTrue(result2.violations.isEmpty());
            assertTrue(result2.correctlyAppliedLaws.isEmpty());
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(4);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(4).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            Question question2 = questionService.solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2.getQuestionData().getId());
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            val question2Responses = questionService.responseQuestion(question2, List.of(0, 1));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question2, question2Responses, tags);
            assertEquals(1, result.violations.size());
            assertEquals(0, result.correctlyAppliedLaws.size());
            assertEquals(0, result.CountCorrectOptions);
            assertEquals(1, result.IterationsLeft);
            assertEquals(
                    "error_base_higher_precedence_right",
                    result.violations.get(0).getLawName());
            float grade = strategy.grade(testExerciseAttemptList.get(4));
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(4);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(4).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            questionService.saveQuestion(question1.questionData);
            Question question2 = questionService.solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2.getQuestionData().getId());
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            val question2Responses = questionService.responseQuestion(question2, List.of(1));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question2, question2Responses, tags);
            assertEquals(0, result.violations.size());
            assertEquals(1, result.correctlyAppliedLaws.size());
            assertEquals("error_base_higher_precedence_right", result.correctlyAppliedLaws.get(0));
            assertEquals(1, result.CountCorrectOptions);
            assertEquals(1, result.IterationsLeft);

            //Сохранение интеракции
            InteractionEntity ie = new InteractionEntity();
            ie.setQuestion(question1.questionData);
            ie.setInteractionType(InteractionType.SEND_RESPONSE);//Какой нужен?
            ie.setViolations(result.violations);
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
            interactionRepository.save(ie);
            ie = interactionRepository.findById(ie.getId()).get();
            ArrayList<InteractionEntity> ies = new ArrayList<>();
            if(question1.questionData.getInteractions() != null) {
                ies.addAll(question1.questionData.getInteractions());
            }
            ies.add(ie);
            question1.questionData.setInteractions(ies);
            questionService.saveQuestion(question1.questionData);

            float grade = strategy.grade(testExerciseAttemptList.get(4));
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(4);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(4).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            Question question2 = questionService.solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2.getQuestionData().getId());
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            val question2Responses = questionService.responseQuestion(question2, List.of(1, 0));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question2, question2Responses, tags);
            assertEquals(0, result.violations.size());
            assertEquals(0, result.correctlyAppliedLaws.size());
            assertEquals(0, result.CountCorrectOptions);
            assertEquals(0, result.IterationsLeft);

            float grade = strategy.grade(testExerciseAttemptList.get(4));
        }

        // Python question. Contrary to C++ "==" and "<" has same precedence
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(5);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(5).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            Question question2 = questionService.solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2.getQuestionData().getId());
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            val question2Responses = questionService.responseQuestion(question2, List.of(0));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question2, question2Responses, tags);
            assertEquals(0, result.violations.size());
            assertEquals(1, result.correctlyAppliedLaws.size());
            assertEquals("error_base_same_precedence_left_associativity_left", result.correctlyAppliedLaws.get(0));
            assertEquals(1, result.CountCorrectOptions);
            assertEquals(1, result.IterationsLeft);

        }

        // Python question. Contrary to C++ "==" and "<" has same precedence
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(5);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(5).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a == b < c"), question1.getQuestionText().getText());
            Question question2 = questionService.solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2.getQuestionData().getId());
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(2, processSolutionResult.IterationsLeft);
            val question2Responses = questionService.responseQuestion(question2, List.of(1));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question2, question2Responses, tags);
            assertEquals(1, result.violations.size());
            assertEquals(0, result.correctlyAppliedLaws.size());
            assertEquals(1, result.CountCorrectOptions);
            assertEquals(2, result.IterationsLeft);

            assertEquals(
                    "error_base_same_precedence_left_associativity_left",
                    result.violations.get(0).getLawName());
            List<HyperText> explanations  = questionService.explainViolations(question2, result.violations);
            assertEquals(1, explanations.size());
            assertEquals("operator == at pos 2 should be evaluated before operator < at pos 4\n" +
                    " because operator == has the same precedence and left associativity", explanations.get(0).getText());
        }

        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(7);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(7).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a + b + c * d"), question1.getQuestionText().getText());
            Question question2 = questionService.solveQuestion(question1, tags);
            val question2Responses = questionService.responseQuestion(question2, List.of(0));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question2, question2Responses, tags).violations;
            assertTrue(mistakes.isEmpty());
        }

        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(7);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(7).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a + b + c * d"), question1.getQuestionText().getText());
            Question question2 = questionService.solveQuestion(question1, tags);
            val question2Responses = questionService.responseQuestion(question2, List.of(1));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question2, question2Responses, tags);
            List<ViolationEntity> mistakes = result.violations;
            assertEquals(2, mistakes.size());
            HashSet<String> expected = new HashSet<>();
            expected.add("error_base_higher_precedence_right");
            expected.add("error_base_same_precedence_left_associativity_left");

            HashSet<String> real = new HashSet<>();
            real.add(mistakes.get(0).getLawName());
            real.add(mistakes.get(1).getLawName());
            assertEquals(expected, real);

            //Сохранение интеракции
            InteractionEntity ie = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question1.questionData,
                    result.violations,
                    result.correctlyAppliedLaws,
                    question2Responses
            );
            ArrayList<InteractionEntity> ies = new ArrayList<>();
            if(question1.questionData.getInteractions() != null) {
                ies.addAll(question1.questionData.getInteractions());
            }
            ies.add(ie);
            question1.questionData.setInteractions(ies);
            questionService.saveQuestion(question1.questionData);

            Long supQ = questionService.generateSupplementaryQuestion(question1.getQuestionData(), result.violations.get(0)).getQuestionData().getId();
            Question supQ2 = questionService.getQuestion(supQ);
            assertEquals(QuestionType.SINGLE_CHOICE, supQ2.getQuestionType());
            assertEquals("OrderOperatorsSupplementary", supQ2.getQuestionDomainType());
            val question2Responses2 = questionService.responseQuestion(supQ2, List.of(1));
            Domain.InterpretSentenceResult resSup = questionService.judgeSupplementaryQuestion(supQ2, question2Responses2, testExerciseAttemptList.get(4));
            assertEquals("select_associativity_type_left", resSup.violations.get(0).getLawName());
            assertNull(resSup.violations.get(0).getDetailedLawName());
            assertTrue(resSup.isAnswerCorrect);

            Long supQ3 = questionService.generateSupplementaryQuestion(question1.getQuestionData(), resSup.violations.get(0)).getQuestionData().getId();
            Question supQ4 = questionService.getQuestion(supQ3);
            val question2Responses3 = questionService.responseQuestion(supQ4, List.of(0));
            Domain.InterpretSentenceResult resSup2 = questionService.judgeSupplementaryQuestion(supQ4, question2Responses3, testExerciseAttemptList.get(4));
            assertEquals("correct_select_associativity_type_left_left", resSup2.violations.get(0).getLawName());
            assertNull(resSup2.violations.get(0).getDetailedLawName());
            assertTrue(resSup2.isAnswerCorrect);

            Long supQ31 = questionService.generateSupplementaryQuestion(question1.getQuestionData(), resSup2.violations.get(0)).getQuestionData().getId();
            Question supQ41 = questionService.getQuestion(supQ31);
            assertTrue(supQ41.getAnswerObjects().isEmpty());
            assertTrue(supQ41.getQuestionText().toString().startsWith("<div class='comp-ph-question'>Yes, operator + has left associativity"));

            Long supQ5 = questionService.generateSupplementaryQuestion(question1.getQuestionData(), result.violations.get(0)).getQuestionData().getId();
            Question supQ6 = questionService.getQuestion(supQ5);
            val question2Responses4 = questionService.responseQuestion(supQ6, List.of(0));
            Domain.InterpretSentenceResult resSup3 = questionService.judgeSupplementaryQuestion(supQ6, question2Responses4, testExerciseAttemptList.get(4));
            assertEquals("select_precedence_or_associativity_left_influence", resSup3.violations.get(0).getLawName());
            assertNull(resSup3.violations.get(0).getDetailedLawName());

            Long supQ7 = questionService.generateSupplementaryQuestion(question1.getQuestionData(), resSup3.violations.get(0)).getQuestionData().getId();
            Question supQ8 = questionService.getQuestion(supQ7);
            val question2Responses5 = questionService.responseQuestion(supQ8, List.of(0));
            Domain.InterpretSentenceResult resSup4 = questionService.judgeSupplementaryQuestion(supQ8, question2Responses5, testExerciseAttemptList.get(4));
            assertEquals("select_highest_precedence_left_operator", resSup4.violations.get(0).getLawName());
            assertNull(resSup4.violations.get(0).getDetailedLawName());

            Long supQ9 = questionService.generateSupplementaryQuestion(question1.getQuestionData(), resSup4.violations.get(0)).getQuestionData().getId();
            Question supQ10 = questionService.getQuestion(supQ9);
            val question2Responses6 = questionService.responseQuestion(supQ10, List.of(0));
            Domain.InterpretSentenceResult resSup5 = questionService.judgeSupplementaryQuestion(supQ10, question2Responses6, testExerciseAttemptList.get(4));
            assertEquals("error_select_associativity_or_arity_influence_left", resSup5.violations.get(0).getLawName());
            assertEquals("error_select_arity_or_associativity", resSup5.violations.get(0).getDetailedLawName());
        }

        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(7);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(7).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a + b + c * d"), question1.getQuestionText().getText());
            Question question2 = questionService.solveQuestion(question1, tags);
            val question2Responses = questionService.responseQuestion(question2, List.of(0));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question2, question2Responses, tags);
            assertTrue(result.violations.isEmpty());

            //Сохранение интеракции
            InteractionEntity ie = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question2.questionData,
                    result.violations,
                    result.correctlyAppliedLaws,
                    question2Responses
            );

            ArrayList<InteractionEntity> ies = new ArrayList<>();
            if(question2.questionData.getInteractions() != null) {
                ies.addAll(question1.questionData.getInteractions());
            }
            ies.add(ie);
            question2.questionData.setInteractions(ies);
            questionService.saveQuestion(question2.questionData);

            // add feedback
            val grade = strategy.grade(attempt);
            ie.getFeedback().setInteractionsLeft(result.IterationsLeft);
            ie.getFeedback().setGrade(grade);
            feedbackRepository.save(ie.getFeedback());

            question2Responses.addAll(questionService.responseQuestion(question2, List.of(1)));
            Domain.InterpretSentenceResult result2 = questionService.judgeQuestion(question2, question2Responses, tags);
            assertEquals(1, result2.violations.size());
            assertEquals("error_base_higher_precedence_right", result2.violations.get(0).getLawName());

            //Сохранение интеракции
            InteractionEntity ie2 = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question2.questionData,
                    result2.violations,
                    result2.correctlyAppliedLaws,
                    question2Responses
            );

            ArrayList<InteractionEntity> ies2 = new ArrayList<>();
            if(question2.questionData.getInteractions() != null) {
                ies2.addAll(question2.questionData.getInteractions());
            }
            ies2.add(ie2);
            question2.questionData.setInteractions(ies2);
            questionService.saveQuestion(question2.questionData);

            Long supQ = questionService.generateSupplementaryQuestion(question2.getQuestionData(), result2.violations.get(0)).getQuestionData().getId();
            Question supQ2 = questionService.getQuestion(supQ);
            assertEquals(QuestionType.SINGLE_CHOICE, supQ2.getQuestionType());
            assertEquals("OrderOperatorsSupplementary", supQ2.getQuestionDomainType());
            val question2Responses3 = questionService.responseQuestion(supQ2, List.of(4));
            Domain.InterpretSentenceResult resSup = questionService.judgeSupplementaryQuestion(supQ2, question2Responses3, testExerciseAttemptList.get(4));
            assertEquals("select_precedence_or_associativity_right_influence", resSup.violations.get(0).getLawName());
            assertNull(resSup.violations.get(0).getDetailedLawName());
            assertFalse(resSup.isAnswerCorrect);
        }

        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(9);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(9).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a + b + c"), question1.getQuestionText().getText());
            Question question2 = questionService.solveQuestion(question1, tags);
            val question2Responses = questionService.responseQuestion(question2, List.of(0));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question2, question2Responses, tags).violations;
            assertTrue(mistakes.isEmpty());
        }

        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(9);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(9).getExercise().getTags();
            assertEquals("<p>Press the operators in the expression in the order they are evaluated</p>" + ProgrammingLanguageExpressionDomain.ExpressionToHtml("a + b + c"), question1.getQuestionText().getText());
            Question question2 = questionService.solveQuestion(question1, tags);
            val question2Responses = questionService.responseQuestion(question2, List.of(1));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question2, question2Responses, tags).violations;
            assertEquals(1, mistakes.size());
            assertEquals(
                    "error_base_same_precedence_left_associativity_left",
                    mistakes.get(0).getLawName());
        }

        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(11);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            Tag tag = new Tag();
            tag.setName("type");
            List<Tag> tags = new ArrayList<>(Arrays.asList(tag));
            assertEquals("MATCHING", question1.getQuestionType().name());
            Question question2 = questionService.solveQuestion(question1, tags);
            Question question3 = getQuestion(question2.getQuestionData().getId());
            val responses = new ArrayList<ResponseEntity>();
            responses.add(makeResponse(question3.getAnswerObject(2), question3.getAnswerObject(8)));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question3, responses, tags).violations;
            assertEquals(0, mistakes.size());
        }

        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(14);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(14).getExercise().getTags();
            assertEquals("MATCHING", question1.getQuestionType().name());
            Question question2 = questionService.solveQuestion(question1, tags);
            Question question3 = getQuestion(question2.getQuestionData().getId());
            val responses = new ArrayList<ResponseEntity>();
            responses.add(makeResponse(question3.getAnswerObject(0), question3.getAnswerObject(5)));
            responses.add(makeResponse(question3.getAnswerObject(1), question3.getAnswerObject(7)));
            responses.add(makeResponse(question3.getAnswerObject(2), question3.getAnswerObject(7)));
            responses.add(makeResponse(question3.getAnswerObject(3), question3.getAnswerObject(8)));
            responses.add(makeResponse(question3.getAnswerObject(4), question3.getAnswerObject(9)));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question3, responses, tags).violations;
            assertEquals(0, mistakes.size());
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(14);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(14).getExercise().getTags();
            assertEquals("MATCHING", question1.getQuestionType().name());
            Question question2 = questionService.solveQuestion(question1, tags);
            Question question3 = getQuestion(question2.getQuestionData().getId());
            val responses = new ArrayList<ResponseEntity>();
            responses.add(makeResponse(question3.getAnswerObject(0), question3.getAnswerObject(6)));
            responses.add(makeResponse(question3.getAnswerObject(1), question3.getAnswerObject(8)));
            responses.add(makeResponse(question3.getAnswerObject(2), question3.getAnswerObject(8)));
            responses.add(makeResponse(question3.getAnswerObject(3), question3.getAnswerObject(9)));
            responses.add(makeResponse(question3.getAnswerObject(4), question3.getAnswerObject(5)));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question3, responses, tags).violations;
            assertEquals(5, mistakes.size());
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(15);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(15).getExercise().getTags();
            assertEquals("MATCHING", question1.getQuestionType().name());
            Question question2 = questionService.solveQuestion(question1, tags);
            Question question3 = getQuestion(question2.getQuestionData().getId());
            val responses = new ArrayList<ResponseEntity>();
            responses.add(makeResponse(question3.getAnswerObject(0), question3.getAnswerObject(3)));
            responses.add(makeResponse(question3.getAnswerObject(1), question3.getAnswerObject(4)));
            responses.add(makeResponse(question3.getAnswerObject(2), question3.getAnswerObject(5)));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question3, responses, tags).violations;
            assertEquals(0, mistakes.size());
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(15);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(15).getExercise().getTags();
            assertEquals("MATCHING", question1.getQuestionType().name());
            Question question2 = questionService.solveQuestion(question1, tags);
            Question question3 = getQuestion(question2.getQuestionData().getId());
            val responses = new ArrayList<ResponseEntity>();
            responses.add(makeResponse(question3.getAnswerObject(0), question3.getAnswerObject(4)));
            responses.add(makeResponse(question3.getAnswerObject(1), question3.getAnswerObject(5)));
            responses.add(makeResponse(question3.getAnswerObject(2), question3.getAnswerObject(3)));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question3, responses, tags).violations;
            assertEquals(3, mistakes.size());
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(16);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(16).getExercise().getTags();
            assertEquals("MATCHING", question1.getQuestionType().name());
            Question question2 = questionService.solveQuestion(question1, tags);
            Question question3 = getQuestion(question2.getQuestionData().getId());
            val responses = new ArrayList<ResponseEntity>();
            responses.add(makeResponse(question3.getAnswerObject(0), question3.getAnswerObject(5)));
            responses.add(makeResponse(question3.getAnswerObject(1), question3.getAnswerObject(5)));
            responses.add(makeResponse(question3.getAnswerObject(2), question3.getAnswerObject(5)));
            responses.add(makeResponse(question3.getAnswerObject(3), question3.getAnswerObject(6)));
            responses.add(makeResponse(question3.getAnswerObject(4), question3.getAnswerObject(7)));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question3, responses, tags).violations;
            assertEquals(0, mistakes.size());
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(16);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(16).getExercise().getTags();
            assertEquals("MATCHING", question1.getQuestionType().name());
            Question question2 = questionService.solveQuestion(question1, tags);
            Question question3 = getQuestion(question2.getQuestionData().getId());
            val responses = new ArrayList<ResponseEntity>();
            responses.add(makeResponse(question3.getAnswerObject(0), question3.getAnswerObject(6)));
            responses.add(makeResponse(question3.getAnswerObject(1), question3.getAnswerObject(7)));
            responses.add(makeResponse(question3.getAnswerObject(2), question3.getAnswerObject(6)));
            responses.add(makeResponse(question3.getAnswerObject(3), question3.getAnswerObject(5)));
            responses.add(makeResponse(question3.getAnswerObject(4), question3.getAnswerObject(6)));
            List<ViolationEntity> mistakes = questionService.judgeQuestion(question3, responses, tags).violations;
            assertEquals(5, mistakes.size());
        }
        {
            ExerciseAttemptEntity attempt = testExerciseAttemptList.get(17);
            QuestionRequest qr = strategy.generateQuestionRequest(attempt);
            checkQuestionRequest(qr, attempt);
            Question question1 = questionService.generateQuestion(attempt);
            assertNotNull(question1);
            List<Tag> tags = testExerciseAttemptList.get(4).getExercise().getTags();
            questionService.saveQuestion(question1.questionData);
            Question question2 = questionService.solveQuestion(question1, tags);
            Domain.ProcessSolutionResult processSolutionResult = getSolveInfo(question2.getQuestionData().getId());
            assertEquals(1, processSolutionResult.CountCorrectOptions);
            assertEquals(5, processSolutionResult.IterationsLeft);
            Domain.CorrectAnswer correctAnswer = questionService.getNextCorrectAnswer(question2);
            assertEquals("operator_binary_*", correctAnswer.answers.get(correctAnswer.answers.size() - 1).getLeft().getConcept());
            assertEquals("single_token_binary_execution", correctAnswer.lawName);
            assertEquals("Operator * at pos 4 evaluates \n" +
                    "before operator + at pos 2: operator * has higher precedence than operator +\n" +
                    "before operator || at pos 6: operator * has higher precedence than operator ||\n" +
                    "before operator + at pos 8: operator * at pos 4 belongs to the left operand of operator || at pos 6 " +
                    "while operator + at pos 8 to its right operand, and the left operand of operator || " +
                    "evaluates before its right operand\n", correctAnswer.explanation.toString());
            Question question3 = getQuestion(question2.getQuestionData().getId());

            val responses = new ArrayList<ResponseEntity>();
            responses.add(makeResponse(correctAnswer.answers.get(correctAnswer.answers.size() - 1).getLeft()));
            Domain.InterpretSentenceResult result = questionService.judgeQuestion(question3, responses, tags);
            //Сохранение интеракции
            InteractionEntity ie = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question3.questionData,
                    result.violations,
                    result.correctlyAppliedLaws,
                    responses
            );
            ArrayList<InteractionEntity> ies = new ArrayList<>();
            if(question3.questionData.getInteractions() != null) {
                ies.addAll(question3.questionData.getInteractions());
            }
            ies.add(ie);
            question3.questionData.setInteractions(ies);
            assertTrue(result.violations.isEmpty());
            questionService.saveQuestion(question3.questionData);

            Long question4 = question3.getQuestionData().getId();
            Domain.CorrectAnswer correctAnswer2 = questionService.getNextCorrectAnswer(question3);
            assertEquals("operator_binary_+", correctAnswer2.answers.get(correctAnswer2.answers.size() - 1).getLeft().getConcept());
            assertEquals("single_token_binary_execution", correctAnswer2.lawName);
            assertEquals("Operator + at pos 2 evaluates \n" +
                    "after operator * at pos 4: operator + has lower precedence than operator *\n" +
                    "before operator + at pos 8: operator + at pos 2 belongs to the left operand of operator || at pos 6 " +
                    "while operator + at pos 8 to its right operand, and the left operand of operator || " +
                    "evaluates before its right operand\n",
                    correctAnswer2.explanation.toString());
            Question question5 = getQuestion(question4);
            responses.add(makeResponse(correctAnswer2.answers.get(correctAnswer2.answers.size() - 1).getLeft()));
            Domain.InterpretSentenceResult result2 = questionService.judgeQuestion(question5, responses, tags);
            //Сохранение интеракции
            InteractionEntity ie2 = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question5.questionData,
                    result2.violations,
                    result2.correctlyAppliedLaws,
                    responses
            );
            ArrayList<InteractionEntity> ies2 = new ArrayList<>();
            if(question5.questionData.getInteractions() != null) {
                ies2.addAll(question5.questionData.getInteractions());
            }
            ies2.add(ie2);
            question5.questionData.setInteractions(ies2);
            questionService.saveQuestion(question5.questionData);

            assertTrue(result2.violations.isEmpty());

            Long question6 = question5.getQuestionData().getId();
            Domain.CorrectAnswer correctAnswer3 = questionService.getNextCorrectAnswer(question5);
            assertEquals("operator_binary_*", correctAnswer3.answers.get(correctAnswer3.answers.size() - 1).getLeft().getConcept());
            assertEquals("single_token_binary_execution", correctAnswer3.lawName);
            assertEquals("Operator * at pos 10 evaluates \n" +
                    "after operator + at pos 2: operator * at pos 10 belongs to the right operand of operator || at pos 6 " +
                    "while operator + at pos 2 to its left operand, and the left operand of operator || " +
                    "evaluates before its right operand\n" +
                    "before operator + at pos 8: operator * has higher precedence than operator +\n",
                    correctAnswer3.explanation.toString());
            Question question7 = getQuestion(question6);
            responses.add(makeResponse(correctAnswer3.answers.get(correctAnswer3.answers.size() - 1).getLeft()));
            Domain.InterpretSentenceResult result3 = questionService.judgeQuestion(question7, responses, tags);
            //Сохранение интеракции
            InteractionEntity ie3 = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question5.questionData,
                    result3.violations,
                    result3.correctlyAppliedLaws,
                    responses
            );
            ArrayList<InteractionEntity> ies3 = new ArrayList<>();
            if(question7.questionData.getInteractions() != null) {
                ies3.addAll(question7.questionData.getInteractions());
            }
            ies3.add(ie3);
            question7.questionData.setInteractions(ies3);
            questionService.saveQuestion(question7.questionData);

            assertTrue(result3.violations.isEmpty());

            Long question8 = question7.getQuestionData().getId();
            Domain.CorrectAnswer correctAnswer4 = questionService.getNextCorrectAnswer(question7);
            assertEquals("operator_binary_+", correctAnswer4.answers.get(correctAnswer4.answers.size() - 1).getLeft().getConcept());
            assertEquals("single_token_binary_execution", correctAnswer4.lawName);
            assertEquals("Operator + at pos 8 evaluates \n" +
                            "after operator + at pos 2: operator + at pos 8 belongs to the right operand of operator || at pos 6 " +
                            "while operator + at pos 2 to its left operand, and the left operand of operator || " +
                            "evaluates before its right operand\n" +
                            "before operator || at pos 6: operator + has higher precedence than operator ||\n" +
                            "after operator * at pos 10: operator + has lower precedence than operator *\n",
                    correctAnswer4.explanation.toString());
            Question question9 = getQuestion(question8);
            responses.add(makeResponse(correctAnswer4.answers.get(correctAnswer4.answers.size() - 1).getLeft()));
            Domain.InterpretSentenceResult result4 = questionService.judgeQuestion(question9, responses, tags);
            //Сохранение интеракции
            InteractionEntity ie4 = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question9.questionData,
                    result4.violations,
                    result4.correctlyAppliedLaws,
                    responses
            );
            ArrayList<InteractionEntity> ies4 = new ArrayList<>();
            if(question9.questionData.getInteractions() != null) {
                ies4.addAll(question9.questionData.getInteractions());
            }
            ies4.add(ie4);
            question9.questionData.setInteractions(ies4);
            questionService.saveQuestion(question9.questionData);

            assertTrue(result4.violations.isEmpty());

            Long question10 = question9.getQuestionData().getId();
            Domain.CorrectAnswer correctAnswer5 = questionService.getNextCorrectAnswer(question9);
            assertEquals("operator_binary_||", correctAnswer5.answers.get(correctAnswer5.answers.size() - 1).getLeft().getConcept());
            assertEquals("single_token_binary_execution", correctAnswer5.lawName);
            assertEquals("Operator || at pos 6 evaluates \n" +
                            "after operator * at pos 4: operator || has lower precedence than operator *\n" +
                            "after operator + at pos 8: operator || has lower precedence than operator +\n",
                    correctAnswer5.explanation.toString());
            Question question11 = getQuestion(question10);
            responses.add(makeResponse(correctAnswer5.answers.get(correctAnswer5.answers.size() - 1).getLeft()));
            Domain.InterpretSentenceResult result5 = questionService.judgeQuestion(question11, responses, tags);
            //Сохранение интеракции
            InteractionEntity ie5 = new InteractionEntity(
                    InteractionType.SEND_RESPONSE,
                    question11.questionData,
                    result5.violations,
                    result5.correctlyAppliedLaws,
                    responses
            );
            ArrayList<InteractionEntity> ies5 = new ArrayList<>();
            if(question11.questionData.getInteractions() != null) {
                ies5.addAll(question11.questionData.getInteractions());
            }
            ies5.add(ie5);
            question11.questionData.setInteractions(ies5);
            questionService.saveQuestion(question11.questionData);

            assertTrue(result5.violations.isEmpty());
            assertTrue(result5.correctlyAppliedLaws.isEmpty());
        }*/
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

    Question getQuestion(Long questionId) {
        Question question = questionService.generateBusinessLogicQuestion(questionRepository.findById(questionId).get());
        assertNotNull(question);
        assertNotNull(question.questionData.getId());
        return question;
    }

    Domain.ProcessSolutionResult getSolveInfo(Long questionId) {
        Question question = getQuestion(questionId);
        Domain domain = DomainAdapter.getDomain(question.questionData.getDomainEntity().getClassPath());
        return domain.processSolution(question.getSolutionFacts());
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