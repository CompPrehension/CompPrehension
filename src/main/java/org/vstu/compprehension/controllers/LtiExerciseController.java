package org.vstu.compprehension.controllers;


import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.var;
import net.oauth.server.OAuthServlet;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jena.shared.NotFoundException;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.*;
import lombok.val;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.utils.Mapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("lti")
@Log4j2
public class LtiExerciseController extends BasicExerciseController {
    @Value("${config.property.lti_launch_secret}")
    private String ltiLaunchSecret;

    @Autowired
    private UserService userService;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @SneakyThrows
    @RequestMapping(value = {"/pages/exercise" }, method=RequestMethod.POST)
    public String ltiLaunch(Model model, HttpServletRequest request, @RequestParam Map<String, String> requestParams) {
        // read LTI data from both request body and request params
        // we can't just extract full form data explicitly through `@RequestParam` or something
        // because we've already cached request and broken formdata->requestparams implicit conversion
        val decodeCodec = new URLCodec();
        val rawBody = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
        val params = Arrays.stream(rawBody.split("&"))
            .map(x -> x.split("="))
            .collect(Collectors.toMap(x -> x[0], x -> { try { return decodeCodec.decode(x[1]); } catch (Exception e) { return ""; }}));
        params.putAll(requestParams);

        // ensure LTI session validity
        val ltiVerifier = new LtiOauthVerifier();
        val secret = this.ltiLaunchSecret;
        val ltiPreparedUrl = OAuthServlet.getMessage(request, null).URL; // special LTI url for correct own `secret` generation
        val ltiResult = ltiVerifier.verifyParameters(params, ltiPreparedUrl, request.getMethod(), secret);
        if (!ltiResult.getSuccess()) {
            model.addAttribute("exerciseLaunchError", "Invalid LTI session. " + ltiResult.getError());
            log.error("Invalid LTI session. " + ltiResult.getMessage());
            return "index";
        }

        var session = request.getSession();
        if (!session.isNew()) {
            session.invalidate();
            session = request.getSession();
        }
        session.setAttribute("ltiSessionInfo", params);

        val exerciseId = List.of("custom_exerciseId", "exerciseId").stream()
                .map(prop -> params.getOrDefault(prop, "-1"))
                .map(vl -> NumberUtils.toLong(vl, -1L))
                .filter(v -> v != -1L)
                .findFirst()
                .orElse(-1L);
        if (exerciseId == -1L) {
            log.error("Param 'custom_exerciseId' or 'exerciseId' is required");
            model.addAttribute("exerciseLaunchError", "Param 'custom_exerciseId' or 'exerciseId' is required");
            return "index";
        }

        return super.launch(model, exerciseId, request);
    }

    @Override
    public String launch(Model model, Long exerciseId, HttpServletRequest request) {
        log.error("No LTI context found");
        return super.launch(model, exerciseId, request);
    }

    @Override
    public UserInfoDto getCurrentUser(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        val currentUserInfo = (UserInfoDto)session.getAttribute("currentUserInfo");
        if (currentUserInfo != null) {
            return currentUserInfo;
        }

        val params = (Map<String, String>) session.getAttribute("ltiSessionInfo");
        if (params == null) {
            throw new Exception("Couldn't get lti session info");
        }
        val userEntity = userService.createOrUpdateFromLti(params);
        val userEntityDto = Mapper.toDto(userEntity);
        session.setAttribute("currentUserInfo", userEntityDto);
        session.setAttribute("currentUserId", userEntityDto.getId());
        return userEntityDto;
    }

    @Override
    public SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        val currentSessionInfo = (SessionInfoDto)session.getAttribute("sessionInfo");
        if (currentSessionInfo != null) {
            return currentSessionInfo;
        }

        val ltiParams = (Map<String, String>) session.getAttribute("ltiSessionInfo");
        if (ltiParams == null) {
            throw new Exception("Couldn't get session info");
        }
        val exerciseId = (Long)session.getAttribute("exerciseId");
        val exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("exercise"));
        val user = getCurrentUser(request);
        val language = ltiParams.getOrDefault("launch_presentation_locale", "EN").toUpperCase();
        val sessionInfo = SessionInfoDto.builder()
                .sessionId(session.getId())
                .exercise(new ExerciseInfoDto(exerciseId, exercise.getOptions()))
                .user(user)
                .language(language)
                .build();
        session.setAttribute("sessionInfo", sessionInfo);

        return sessionInfo;
    }
}
