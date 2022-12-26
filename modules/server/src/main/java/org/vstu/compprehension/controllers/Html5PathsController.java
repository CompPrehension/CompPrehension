package org.vstu.compprehension.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class Html5PathsController {

    @RequestMapping( method = {RequestMethod.OPTIONS, RequestMethod.GET}, path = {"/pages/**", "/"} )
    public String forwardPaths() {
        return "forward:/index.html";
    }
}
