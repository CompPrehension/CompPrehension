package com.example.demo.controllers;


import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Service.DomainService;
import com.example.demo.dto.QuestionAnswerDto;
import com.example.demo.dto.QuestionDto;
import com.example.demo.dto.SessionInfoDto;
import com.example.demo.dto.UserInfoDto;
import com.example.demo.models.businesslogic.Domain;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.businesslogic.QuestionRequest;
import com.example.demo.models.businesslogic.Strategy;
import com.example.demo.models.entities.ExerciseAttempt;
import com.example.demo.models.repository.ExerciseAttemptRepository;
import com.example.demo.utils.DomainAdapter;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Controller
@RequestMapping("lti")
public class LtiController {
    @Value("${config.property.lti_launch_secret}")
    private String ltiLaunchSecret;

    @Autowired
    private Strategy strategy;

    @Autowired
    private DomainService domainService;

    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;


    Question generateQuestion(ExerciseAttempt exerciseAttempt) {
        Domain domain = DomainAdapter.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        Question question = domain.makeQuestion(qr, exerciseAttempt.getUser().getPreferred_language());
        return question;
    }

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


    @RequestMapping(value = {"/getQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    public QuestionDto getQuestion(@RequestParam(name = "question_id") Long exAttemptId) throws Exception {
        ExerciseAttempt attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));
        Question question = generateQuestion(attempt);
        com.example.demo.models.entities.Question qData = question.getQuestionData();

        QuestionDto dto = new QuestionDto();
        dto.setId(exAttemptId.toString());
        dto.setType(qData.getQuestionType().ordinal());
        dto.setAnswers(new QuestionAnswerDto[0]);

        StringBuilder sb = new StringBuilder();
        int idx = 0;
        sb.append(qData.getQuestionText().replaceAll("[\\+\\-\\*\\/]", "<span id='answer_0' class='comp-ph-expr-op-btn'>$0</span>")
                                         .replaceAll("\\*", "&#8727"));
        sb.insert(0, "<p class='comp-ph-expr'>"); sb.append("</p>");
        sb.insert(0, "<p>Приоритет операций в порядке убывания</p>");
        sb.insert(0, "<div class='comp-ph-question'>"); sb.append("</div>");
        dto.setText(sb.toString());

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

        SessionInfoDto result = new SessionInfoDto();
        result.setSessionId(session.getId());
        result.setAttemptIds(StreamSupport.stream(exerciseAttemptRepository.findAll().spliterator(), false)
            .map(v -> v.getId()).map(v -> v.toString())
            .toArray(String[]::new));
        result.setUser(user);
        result.setExpired(new Date());

        return result;
    }
}
