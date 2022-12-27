package org.vstu.compprehension.controllers;

import lombok.SneakyThrows;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class Html5PathsController {
    @SneakyThrows
    @RequestMapping( method = {RequestMethod.OPTIONS, RequestMethod.GET, RequestMethod.POST}, path = {"/pages/**", "/"} )
    public String forwardPaths() {
        return "index.html";
    }
}
