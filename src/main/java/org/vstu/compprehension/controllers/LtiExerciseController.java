package org.vstu.compprehension.controllers;


import org.apache.commons.lang3.math.NumberUtils;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
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
public class LtiExerciseController extends BasicExerciseController {
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

        val session = request.getSession();
        session.setAttribute("sessionInfo", params);

        val exerciseIdS = params.getOrDefault("custom_exerciseId", "-1");
        val exerciseId = NumberUtils.toLong(exerciseIdS, -1L);
        if (exerciseId == -1) {
            throw new Exception("Param 'custom_exerciseId' is required");
        }

        return launch(exerciseId, request);
    }

    @Override
    public SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        val params = (Map<String, String>) session.getAttribute("sessionInfo");
        if (params == null) {
            throw new Exception("Couldn't get session info");
        }

        val exerciseId = (Long)session.getAttribute("exerciseId");

        // get or create user
        val userEntity = userService.createOrUpdateFromLti(params);
        session.setAttribute("userId", userEntity.getId());

        val language = params.getOrDefault("launch_presentation_locale", "EN").toUpperCase();
        return SessionInfoDto.builder()
            .sessionId(session.getId())
            .exerciseId(exerciseId)
            .user(Mapper.toDto(userEntity))
            .language(language)
            .build();
    }
}
