package org.vstu.compprehension.controllers;


import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.Strategy;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import lombok.val;
import org.apache.commons.collections4.IterableUtils;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.dto.Mapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("lti")
public class LtiController {
    @Value("${config.property.lti_launch_secret}")
    private String ltiLaunchSecret;

    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private Strategy strategy;


    @RequestMapping(value = {"/launch"}, method = {RequestMethod.POST})
    public String ltiLaunch(Model model, HttpServletRequest request, @RequestParam Map<String, String> params) throws Exception {
        LtiVerifier ltiVerifier = new LtiOauthVerifier();
        String key = request.getParameter("oauth_consumer_key");
        String secret = this.ltiLaunchSecret;
        LtiVerificationResult ltiResult = ltiVerifier.verify(request, secret);
        if (!ltiResult.getSuccess()) {
            throw new Exception("Invalid LTI session");
        }

        HttpSession session = request.getSession();
        session.setAttribute("sessionInfo", params);

        return "index";
    }

    @RequestMapping(value = {"/addAnswer"}, method = { RequestMethod.POST }, produces = "application/json",
            consumes = "application/json")
    @ResponseBody
    public FeedbackDto addAnswer(@RequestBody InteractionDto interaction,
                                 HttpServletRequest request) throws Exception {
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
        float grade = strategy.grade(attempt);

        return FeedbackDto.builder()
            .grade(grade)
            .errors(errors)
            .iterationsLeft(judgeResult.IterationsLeft)
            .correctOptionsCount(judgeResult.CountCorrectOptions)
            .build();
    }


    @RequestMapping(value = {"/getQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    public QuestionDto getQuestion(@RequestParam(name = "attemptId") Long exAttemptId) throws Exception {
        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));
        Question question = questionService.generateQuestion(attempt);
        QuestionEntity qData = question.getQuestionData();
        return Mapper.toDto(qData);
    }

    @RequestMapping(value = {"/loadSessionInfo"}, method = { RequestMethod.GET })
    @ResponseBody
    public SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        val params = (Map<String, String>) session.getAttribute("sessionInfo");
        if (params == null) {
            throw new Exception("Couldn't get session info");
        }

        // get or create user
        val userEntity = userService.createOrUpdateFromLti(params);
        val userDto = Mapper.toDto(userEntity);

        val exercises = IterableUtils.toList(exerciseRepository.findAll());
        val exerciseAttempts = new ArrayList<ExerciseAttemptEntity>();
        for (ExerciseEntity e : exercises) {
            ExerciseAttemptEntity ae = new ExerciseAttemptEntity();
            ae.setExercise(e);
            ae.setAttemptStatus(AttemptStatus.INCOMPLETE);
            ae.setUser(userEntity);
            exerciseAttempts.add(exerciseAttemptRepository.save(ae));
        }

        val language = params.getOrDefault("launch_presentation_locale", "EN").toUpperCase();
        return SessionInfoDto.builder()
            .sessionId(session.getId())
            .attemptIds(exerciseAttempts.stream()
                                        .map(v -> v.getId())
                                        .toArray(Long[]::new))
            .user(userDto)
            .language(language)
            .build();
    }
}
