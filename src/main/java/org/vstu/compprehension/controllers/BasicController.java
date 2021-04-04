package org.vstu.compprehension.controllers;

import lombok.val;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.InteractionService;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.FeedbackDto;
import org.vstu.compprehension.dto.InteractionDto;
import org.vstu.compprehension.utils.Mapper;
import org.vstu.compprehension.dto.SessionInfoDto;
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
public class BasicController implements AbstractFrontController {

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
    private InteractionService interactionService;

    @Override
    public String launch(HttpServletRequest request) throws Exception {
        val exerciseIdS = request.getParameter("exerciseId");
        val exerciseId = NumberUtils.toLong(exerciseIdS, -1L);
        if (exerciseId == -1L) {
            throw new Exception("Invalid 'exerciseId' format");
        }
        val session = request.getSession();
        session.setAttribute("exerciseId", exerciseId);

        return "index";
    }

    @Override
    public FeedbackDto addAnswer(InteractionDto interaction, HttpServletRequest request) throws Exception {
        Long exAttemptId = interaction.getAttemptId();
        Long questionId = interaction.getQuestionId();
        Long[][] answers = interaction.getAnswers();

        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));

        List<Tag> tags = exerciseService.getTags(attempt.getExercise());
        Question question = questionService.getQuestion(questionId);
        questionService.solveQuestion(question, tags);
        questionService.responseQuestion(question, answers);
        Domain.InterpretSentenceResult judgeResult = questionService.judgeQuestion(question, tags);
        List<HyperText> explanations = questionService.explainMistakes(question, judgeResult.mistakes);
        String[] errors = explanations.stream().map(s -> s.getText()).toArray(String[]::new);
        InteractionEntity ie = new InteractionEntity(question.getQuestionData(), judgeResult.mistakes, judgeResult.IterationsLeft,
                judgeResult.correctlyAppliedLaws, question.getResponses());
        interactionService.saveInteraction(ie);

        float grade = strategy.grade(attempt);

        return FeedbackDto.builder()
                .grade(grade)
                .errors(errors)
                .stepsLeft(judgeResult.IterationsLeft)
                .totalSteps(null) // TODO get from interaction
                .build();
    }

    @Override
    public QuestionDto generateQuestion(Long exAttemptId) throws Exception {
        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));
        Question question = questionService.generateQuestion(attempt);
        QuestionEntity qData = question.getQuestionData();
        return Mapper.toDto(qData);
    }

    @Override
    public QuestionDto getQuestion(Long questionId) throws Exception {
        val question = questionService.getQuestionEntiity(questionId);
        return Mapper.toDto(question);
    }

    @Override
    public SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception {
        // get or create user
        val userEntity = userService.createOrUpdateFromAuthentication();

        // get exercise
        val session = request.getSession();
        val exerciseIdS = session.getAttribute("exerciseId").toString();
        val exerciseId = NumberUtils.toLong(exerciseIdS, -1L);
        val exercise = exerciseService.getExercise(exerciseId);

        // check existing attempt
        // TODO ask user whether use existing attempt or create new
        val existingAttempt = exerciseAttemptRepository
                .findFirstByExerciseIdAndUserIdAndAttemptStatus(exerciseId, userEntity.getId(), AttemptStatus.INCOMPLETE);
        val attempt = existingAttempt
                .orElseGet(() -> {
                    val ea = new ExerciseAttemptEntity();
                    ea.setExercise(exercise);
                    ea.setUser(userEntity);
                    ea.setAttemptStatus(AttemptStatus.INCOMPLETE);
                    ea.setQuestions(new ArrayList<>(0));
                    return exerciseAttemptRepository.save(ea);
                });

        return SessionInfoDto.builder()
                .sessionId(session.getId())
                .attemptId(attempt.getId())
                .questionIds(attempt.getQuestions().stream()
                        .map(v -> v.getId())
                        .toArray(Long[]::new))
                .user(Mapper.toDto(userEntity))
                .language("EN")
                .build();
    }
}
