package com.example.demo.controllers;


import com.example.demo.Service.ExerciseService;
import com.example.demo.Service.QuestionService;
import com.example.demo.dto.*;
import com.example.demo.models.businesslogic.Tag;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.AttemptStatus;
import com.example.demo.models.repository.ExerciseAttemptRepository;
import com.example.demo.models.repository.ExerciseRepository;
import com.example.demo.models.repository.UserRepository;
import com.example.demo.utils.HyperText;
import org.apache.commons.collections4.IterableUtils;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private UserRepository userRepository;

    @Autowired
    private QuestionService questionService;


    @RequestMapping(value = {"/launch"}, method = {RequestMethod.POST})
    public String ltiLaunch(Model model, HttpServletRequest request, @RequestParam Map<String, Object> params) throws Exception {
        //List<ExerciseAttempt> testExerciseAttemptList = IterableUtils.toList( exerciseAttemptRepository.findAll());
        //Question question1 = generateQuestion(testExerciseAttemptList.get(0));

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
    public String[] addAnswer(@RequestBody InteractionDto interaction,
                              HttpServletRequest request) throws Exception {
        Long exAttemptId = interaction.getAttemptId();
        List<Integer> answerIds = Arrays.stream(interaction.getAnswers().split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));

        List<Tag> tags = exerciseService.getTags(attempt.getExercise());
        Question question = questionService.generateQuestion(attempt);
        questionService.solveQuestion(question, tags);
        questionService.responseQuestion(question, answerIds);
        List<MistakeEntity> mistakes = questionService.judgeQuestion(question, tags);
        List<HyperText> explanations = questionService.explainMistakes(question, mistakes);
        String[] errors = explanations.stream().map(s -> s.getText()).toArray(String[]::new);
        return errors;
    }


    @RequestMapping(value = {"/getQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    public QuestionDto getQuestion(@RequestParam(name = "attemptId") Long exAttemptId) throws Exception {
        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));
        Question question = questionService.generateQuestion(attempt);
        QuestionEntity qData = question.getQuestionData();

        QuestionDto dto = new QuestionDto();
        dto.setId(exAttemptId.toString());
        dto.setType(qData.getQuestionType().ordinal());
        dto.setAnswers(new QuestionAnswerDto[0]);

        StringBuilder sb = new StringBuilder(qData.getQuestionText());
        Pattern pattern = Pattern.compile("\\<\\=|\\>\\=|\\=\\=|\\!\\=|\\<\\<|\\>\\>|\\+|\\-|\\*|\\/|\\<|\\>|\\w+");
        Matcher matcher = pattern.matcher(sb.toString());
        int idx = 0;
        int anwerIdx = -1;
        int offset = 0;
        while (offset < sb.length() && matcher.find(offset)) {
            String match = matcher.group(0);
            String replaceStr = match.matches("\\w")
                    ? "<span data-comp-ph-pos='" + (++idx) +"' class='comp-ph-expr-const'>" + matcher.group(0) +"</span>"
                    : "<span data-comp-ph-pos='" + (++idx) +"' id='answer_" + (++anwerIdx) +"' class='comp-ph-expr-op-btn'>" + matcher.group(0) +"</span>";

            sb.replace(matcher.start(), matcher.end(), replaceStr);
            offset = matcher.start() + replaceStr.length() ;
            matcher = pattern.matcher(sb.toString());
        }

        sb = new StringBuilder(sb.toString().replaceAll("\\*", "&#8727"));
        sb.insert(0, "<p class='comp-ph-expr'>"); sb.append("</p>");
        sb.insert(0, "<p>Приоритет операций в порядке убывания</p>");
        sb.insert(0, "<div class='comp-ph-question'>"); sb.append("</div>");
        dto.setText(sb.toString());

        dto.setOptions(qData.getOptions());

        return dto;
    }

    @RequestMapping(value = {"/loadSessionInfo"}, method = { RequestMethod.GET })
    @ResponseBody
    public SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        Map<String, Object> params = (Map<String, Object>) session.getAttribute("sessionInfo");
        if (params == null) {
            throw new Exception("Couldn't get session info");
        }

        UserInfoDto user = new UserInfoDto();
        user.setId(params.get("user_id").toString());
        user.setDisplayName(params.get("lis_person_name_given").toString());
        user.setEmail(params.get("lis_person_contact_email_primary").toString());
        user.setRoles(Stream.of(params.get("roles").toString().split(","))
                .map(String::trim)
                .collect(Collectors.toList()));


        List<ExerciseEntity> exercises = IterableUtils.toList(exerciseRepository.findAll());
        List<ExerciseAttemptEntity> exerciseAttempts = new ArrayList<>();
        UserEntity currentUser = userRepository.findAll().iterator().next();
        for (ExerciseEntity e : exercises) {
            ExerciseAttemptEntity ae = new ExerciseAttemptEntity();
            ae.setExercise(e);
            ae.setAttemptStatus(AttemptStatus.INCOMPLETE);
            ae.setUser(currentUser);
            exerciseAttempts.add(exerciseAttemptRepository.save(ae));
        }

        SessionInfoDto result = new SessionInfoDto();
        result.setSessionId(session.getId());
        result.setAttemptIds(exerciseAttempts.stream()
            .map(v -> v.getId()).map(v -> v.toString())
            .toArray(String[]::new));
        result.setUser(user);
        result.setExpired(new Date());

        return result;
    }
}
