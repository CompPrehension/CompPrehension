package org.vstu.compprehension.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("lti")
public class LtiController {
    @RequestMapping( method = {RequestMethod.GET, RequestMethod.POST }, path = {"1_3/login"} )
    public String forwardPaths(HttpServletRequest request) {
        return "forward:/index.html";
    }
}
