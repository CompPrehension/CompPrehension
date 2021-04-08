package org.vstu.compprehension.controllers;

import lombok.val;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.controllers.interfaces.ExerciseAttemptController;
import org.vstu.compprehension.controllers.interfaces.ExerciseStatisticsController;
import org.vstu.compprehension.controllers.interfaces.QuestionController;
import org.vstu.compprehension.controllers.interfaces.SessionController;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.models.repository.FeedbackRepository;
import org.vstu.compprehension.models.repository.InteractionRepository;
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
import java.util.List;

@Controller
@RequestMapping("basic")
public class BasicController implements SessionController, QuestionController,
        ExerciseStatisticsController, ExerciseAttemptController {

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
    public String launch(HttpServletRequest request) throws Exception {
        return "index";
    }

    @Override
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
        questionService.responseQuestion(question, answers);
        Domain.InterpretSentenceResult judgeResult = questionService.judgeQuestion(question, tags);
        List<HyperText> explanations = questionService.explainMistakes(question, judgeResult.mistakes);
        String[] errors = explanations.stream().map(s -> s.getText()).toArray(String[]::new);

        // add interaction
        val existingInteractions = question.getQuestionData().getInteractions();
        val ie = new InteractionEntity(question.getQuestionData(), judgeResult.mistakes, judgeResult.correctlyAppliedLaws, question.getResponses());
        existingInteractions.add(ie);
        val interactionsCount = existingInteractions.size();

        // add feedback
        val grade = strategy.grade(attempt);
        ie.getFeedback().setInteractionsLeft(judgeResult.IterationsLeft);
        ie.getFeedback().setGrade(grade);
        feedbackRepository.save(ie.getFeedback());

        return FeedbackDto.builder()
                .grade(grade)
                .errors(errors)
                .stepsLeft(judgeResult.IterationsLeft)
                .totalSteps(interactionsCount)
                .stepsWithErrors((int)existingInteractions.stream().filter(i -> i.getMistakes().size() > 0).count())
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
    public SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception {
        // get or create user
        val userEntity = userService.createOrUpdateFromAuthentication();
        val session = request.getSession();
        session.setAttribute("userId", userEntity.getId());

        return SessionInfoDto.builder()
                .sessionId(session.getId())
                .user(Mapper.toDto(userEntity))
                .language("EN")
                .build();
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
                            .filter(i -> i.getMistakes().size() > 0).count();
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
    public ExerciseAttemptDto getExistingExerciseAttempt(Long exerciseId, HttpServletRequest request) {
        val session = request.getSession();
        val userId = (Long)session.getAttribute("userId");
        val existingAttempt = exerciseAttemptRepository
                .findFirstByExerciseIdAndUserIdAndAttemptStatus(exerciseId, userId, AttemptStatus.INCOMPLETE);
        return existingAttempt
                .map(Mapper::toDto)
                .orElse(null);
    }

    @Override
    public ExerciseAttemptDto createExerciseAttempt(Long exerciseId, HttpServletRequest request) {
        val session = request.getSession();
        val userId = (Long)session.getAttribute("userId");
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
