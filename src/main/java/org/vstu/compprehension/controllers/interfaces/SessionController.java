package org.vstu.compprehension.controllers.interfaces;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.dto.SessionInfoDto;

import javax.servlet.http.HttpServletRequest;

public interface SessionController {
    @RequestMapping(value = {"/"}, method = { RequestMethod.GET })
    String launch(HttpServletRequest request) throws Exception;

    @RequestMapping(value = {"/pages/**"}, method = { RequestMethod.GET })
    String pages(HttpServletRequest request);

    @RequestMapping(value = {"/loadSessionInfo"}, method = { RequestMethod.GET })
    @ResponseBody
    SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception;
}
