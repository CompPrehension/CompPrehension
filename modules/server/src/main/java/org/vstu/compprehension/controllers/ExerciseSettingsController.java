package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.val;
import net.oauth.server.OAuthServlet;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.models.businesslogic.user.UserContext;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.utils.SessionHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("api")
public class ExerciseSettingsController {
    private final ExerciseRepository exerciseRepository;
    private final ExerciseService exerciseService;
    private final UserContext currentUser;
    private final String ltiLaunchSecret;

    @Autowired
    public ExerciseSettingsController(ExerciseRepository exerciseRepository,
                                      ExerciseService exerciseService,
                                      UserContext currentUser,
                                      @Value("${config.property.lti_launch_secret}") String ltiLaunchSecret) {
        this.exerciseRepository = exerciseRepository;
        this.exerciseService = exerciseService;
        this.currentUser = currentUser;
        this.ltiLaunchSecret = ltiLaunchSecret;
    }

    @RequestMapping(value = { "exercise" }, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id) {
        if (!currentUser.getRoles().contains(Role.TEACHER))
            throw new AuthorizationServiceException("Current user dont have TEACHER rights");
        return exerciseService.getExerciseCard(id);
    }

    @RequestMapping(value = { "exercise"}, method = { RequestMethod.POST })
    @ResponseBody
    public void update(@RequestBody ExerciseCardDto card) {
        if (!currentUser.getRoles().contains(Role.TEACHER))
            throw new AuthorizationServiceException("Current user dont have TEACHER rights");

        exerciseService.saveExerciseCard(card);
    }

    @RequestMapping(value = { "exercise"}, method = { RequestMethod.PUT })
    @ResponseBody
    public long create(@RequestBody ObjectNode json) {
        if (!currentUser.getRoles().contains(Role.TEACHER))
            throw new AuthorizationServiceException("Current user dont have TEACHER rights");

        var name = json.get("name").asText();
        var domainId = json.get("domainId").asText();
        var strategyId = json.get("strategyId").asText();
        return exerciseService.createExercise(name, domainId, strategyId).getId();
    }

    /*
    @SneakyThrows
    @RequestMapping(value = {  "lti/pages/exercise-settings"}, method=RequestMethod.POST)
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
            throw new AuthorizationServiceException("Authentication error");
        }

        var session = SessionHelper.ensureNewSession(request);
        session.setAttribute("ltiSessionInfo", params);
        return "index";
    }
    */
}
