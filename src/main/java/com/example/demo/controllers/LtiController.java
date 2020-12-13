package com.example.demo.controllers;


import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
@RequestMapping("lti")
public class LtiController {
    @Value("${config.property.lti_launch_secret}")
    private String ltiLaunchSecret;

    @RequestMapping(value = {"/launch"}, method = {RequestMethod.POST})
    public String ltiLaunch(Model model, HttpServletRequest request, @RequestParam Map params, HttpServletResponse resp) throws Exception {
        LtiVerifier ltiVerifier = new LtiOauthVerifier();
        String key = request.getParameter("oauth_consumer_key");
        String secret = this.ltiLaunchSecret;
        LtiVerificationResult ltiResult = ltiVerifier.verify(request, secret);
        return "index";
    }
}
