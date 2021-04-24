package org.vstu.compprehension.controllers;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.controllers.interfaces.ExerciseController;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.models.repository.FeedbackRepository;
import org.vstu.compprehension.utils.Mapper;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.Strategy;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.utils.HyperText;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.vstu.compprehension.models.entities.EnumData.InteractionType.REQUEST_CORRECT_ANSWER;
import static org.vstu.compprehension.models.entities.EnumData.InteractionType.SEND_RESPONSE;

@Controller
@RequestMapping("basic")
public class BasicExerciseController implements ExerciseController {

    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private Strategy strategy;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Override
    public String launch(Long exerciseId, HttpServletRequest request) throws Exception {
        val session = request.getSession();
        session.setAttribute("exerciseId", exerciseId);

        if (exerciseId == null) {
            throw new Exception("exerciseId param is required");
        }

        return "index";
    }

    /**
     * Routes all "/pages/**" requests to the corresponding pages
     * @param request Current request
     * @return Html
     */
    @RequestMapping(value = {"/pages/**"}, method = { RequestMethod.GET })
    public String pages(HttpServletRequest request) {
        return "index";
    }

    @Override
    public FeedbackDto addQuestionAnswer(InteractionDto interaction, HttpServletRequest request) throws Exception {
        Long exAttemptId = interaction.getAttemptId();
        Long questionId = interaction.getQuestionId();
        Long[][] answers = interaction.getAnswers();

        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));

        List<Tag> tags = exerciseService.getTags(attempt.getExercise());
        Question question = questionService.getQuestion(questionId);

        // evaluate answer
        questionService.solveQuestion(question, tags);
        val responses = questionService.responseQuestion(question, answers);
        Domain.InterpretSentenceResult judgeResult = questionService.judgeQuestion(question, responses, tags);
        List<HyperText> explanations = questionService.explainViolations(question, judgeResult.violations);
        String[] errors = explanations.stream().map(s -> s.getText()).toArray(String[]::new);

        // add interaction
        val existingInteractions = question.getQuestionData().getInteractions();
        val ie = new InteractionEntity(SEND_RESPONSE, question.getQuestionData(), judgeResult.violations, judgeResult.correctlyAppliedLaws, responses);
        existingInteractions.add(ie);
        val correctInteractionsCount = (int)existingInteractions.stream().filter(i -> i.getViolations().size() == 0).count();

        // add feedback
        val grade = strategy.grade(attempt);
        ie.getFeedback().setInteractionsLeft(judgeResult.IterationsLeft);
        ie.getFeedback().setGrade(grade);
        feedbackRepository.save(ie.getFeedback());

        // remove incorrect answers from feedback
        val correctAnswers = errors.length > 0
                ? Arrays.copyOf(answers, answers.length - 1)
                : answers;

        return FeedbackDto.builder()
                .grade(grade)
                .errors(errors)
                .stepsLeft(judgeResult.IterationsLeft)
                .correctSteps(correctInteractionsCount)
                .stepsWithErrors((int)existingInteractions.stream().filter(i -> i.getViolations().size() > 0).count())
                .correctAnswers(correctAnswers)
                .build();
    }

    @Override
    public QuestionDto generateQuestion(Long exAttemptId, HttpServletRequest request) throws Exception {
        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));
        Question question = questionService.generateQuestion(attempt);
        QuestionEntity qData = question.getQuestionData();
        return Mapper.toDto(qData);
    }

    @Override
    public QuestionDto getQuestion(Long questionId, HttpServletRequest request) throws Exception {
        val question = questionService.getQuestionEntiity(questionId);
        return Mapper.toDto(question);
    }

    @Override
    public FeedbackDto generateNextCorrectAnswer(@RequestParam Long questionId, HttpServletRequest request) throws Exception {
        Question question = questionService.getQuestion(questionId);

        // evaluate answer
        val tags = exerciseService.getTags(question.getQuestionData().getExerciseAttempt().getExercise());
        questionService.solveQuestion(question, tags);

        val correctAnswer = questionService.getNextCorrectAnswer(question);
        val correctAnswerDto = Mapper.toDto(correctAnswer);

        val responses = questionService.responseQuestion(question, correctAnswerDto.getAnswers());
        Domain.InterpretSentenceResult judgeResult = questionService.judgeQuestion(question, responses, tags);

        // add interaction
        val existingInteractions = question.getQuestionData().getInteractions();
        val ie = new InteractionEntity(REQUEST_CORRECT_ANSWER, question.getQuestionData(), judgeResult.violations, judgeResult.correctlyAppliedLaws, responses);
        existingInteractions.add(ie);
        val correctInteractionsCount = (int)existingInteractions.stream().filter(i -> i.getViolations().size() == 0).count();

        // add feedback
        val grade = strategy.grade(question.getQuestionData().getExerciseAttempt());
        ie.getFeedback().setInteractionsLeft(judgeResult.IterationsLeft);
        ie.getFeedback().setGrade(grade);
        feedbackRepository.save(ie.getFeedback());

        return FeedbackDto.builder()
                .grade(grade)
                .errors(new String[0])
                .explanation(correctAnswerDto.getExplanation())
                .correctAnswers(correctAnswerDto.getAnswers())
                .stepsLeft(judgeResult.IterationsLeft)
                .correctSteps(correctInteractionsCount)
                .stepsWithErrors((int)existingInteractions.stream().filter(i -> i.getViolations().size() > 0).count())
                .build();
    }

    @Override
    public SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception {
        val session = request.getSession();
        val currentSessionInfo = (SessionInfoDto)session.getAttribute("sessionInfo");
        if (currentSessionInfo != null) {
            return currentSessionInfo;
        }

        val user = getCurrentUser(request);
        val exerciseId = (Long)session.getAttribute("exerciseId");
        val sessionInfo = SessionInfoDto.builder()
                .sessionId(session.getId())
                .exerciseId(exerciseId)
                .user(user)
                .language("EN")
                .build();
        session.setAttribute("sessionInfo", sessionInfo);

        return sessionInfo;
    }

    @Override
    public UserInfoDto getCurrentUser(HttpServletRequest request) throws Exception {
        val session = request.getSession();
        val currentUserInfo = (UserInfoDto)session.getAttribute("currentUserInfo");
        if (currentUserInfo != null) {
            return currentUserInfo;
        }

        val userEntity = userService.createOrUpdateFromAuthentication();
        val userEntityDto = Mapper.toDto(userEntity);
        session.setAttribute("currentUserInfo", userEntityDto);

        return userEntityDto;
    }


    @Override
    public ExerciseStatisticsItemDto[] getExerciseStatistics(Long exerciseId) {
        val exercise = exerciseService.getExercise(exerciseId);

        val result = exercise.getExerciseAttempts().stream()
                .map(att -> {
                    val questionsCount = att.getQuestions().size();
                    val totalInteractionsCount = att.getQuestions().stream()
                            .filter(q -> q.getInteractions() != null)
                            .flatMap(q -> q.getInteractions().stream()).count();
                    val totalInteractionsWithErrorsCount = att.getQuestions().stream()
                            .filter(q -> q.getInteractions() != null)
                            .flatMap(q -> q.getInteractions().stream())
                            .filter(i -> i.getViolations().size() > 0).count();
                    double avgGrade = att.getQuestions().stream()
                            .filter(q -> q.getInteractions() != null && q.getInteractions().size() > 0)
                            .mapToDouble(q -> {
                                val last = q.getInteractions().stream().reduce((f, s) -> s);
                                val grade = last.map(l -> l.getFeedback()).map(f -> f.getGrade()).orElse(0f);
                                return grade;
                            })
                            .average()
                            .orElse(0d);

                    return ExerciseStatisticsItemDto.builder()
                            .attemptId(att.getId())
                            .averageGrade(avgGrade)
                            .questionsCount(questionsCount)
                            .totalInteractionsCount((int)totalInteractionsCount)
                            .totalInteractionsWithErrorsCount((int)totalInteractionsWithErrorsCount)
                            .build();
                })
                .toArray(ExerciseStatisticsItemDto[]::new);

        return result;
    }

    @Override
    public ExerciseAttemptDto getExistingExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception {
        val userId = getCurrentUser(request).getId();
        val existingAttempt = exerciseAttemptRepository
                .findFirstByExerciseIdAndUserIdAndAttemptStatusOrderByIdDesc(exerciseId, userId, AttemptStatus.INCOMPLETE);
        return existingAttempt
                .map(Mapper::toDto)
                .orElse(null);
    }

    @Override
    public ExerciseAttemptDto createExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception {
        val userId = getCurrentUser(request).getId();
        val exercise = exerciseService.getExercise(exerciseId);
        val user = userService.getUser(userId);

        val ea = new ExerciseAttemptEntity();
        ea.setExercise(exercise);
        ea.setUser(user);
        ea.setAttemptStatus(AttemptStatus.INCOMPLETE);
        ea.setQuestions(new ArrayList<>(0));
        exerciseAttemptRepository.save(ea);

        return Mapper.toDto(ea);
    }
}
