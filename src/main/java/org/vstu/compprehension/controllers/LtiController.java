package org.vstu.compprehension.controllers;


import org.apache.commons.lang3.math.NumberUtils;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import lombok.val;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.utils.Mapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("lti")
public class LtiController extends BasicController {
    @Value("${config.property.lti_launch_secret}")
    private String ltiLaunchSecret;

    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private UserService userService;

    @RequestMapping(value = {"/launch" }, method = {RequestMethod.POST})
    public String ltiLaunch(Model model, HttpServletRequest request, @RequestParam Map<String, String> params) throws Exception {
        LtiVerifier ltiVerifier = new LtiOauthVerifier();
        String secret = this.ltiLaunchSecret;
        LtiVerificationResult ltiResult = ltiVerifier.verify(request, secret);
        if (!ltiResult.getSuccess()) {
            throw new Exception("Invalid LTI session");
        }

        HttpSession session = request.getSession();
        session.setAttribute("sessionInfo", params);

        return "index";
    }

    @Override
    public SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        val params = (Map<String, String>) session.getAttribute("sessionInfo");
        if (params == null) {
            throw new Exception("Couldn't get session info");
        }

        val exerciseId = NumberUtils.toLong(params.getOrDefault("custom_exerciseid", null), -1L);
        if (exerciseId == -1L) {
            throw new Exception("Invalid 'exerciseId' format");
        }
        val exercise = exerciseService.getExercise(exerciseId);

        // get or create user
        val userEntity = userService.createOrUpdateFromLti(params);

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

        val language = params.getOrDefault("launch_presentation_locale", "EN").toUpperCase();
        return SessionInfoDto.builder()
            .sessionId(session.getId())
            .attemptId(attempt.getId())
            .exerciseId(exerciseId)
            .questionIds(attempt.getQuestions().stream()
                                        .map(v -> v.getId())
                                        .toArray(Long[]::new))
            .user(Mapper.toDto(userEntity))
            .language(language)
            .build();
    }
}
