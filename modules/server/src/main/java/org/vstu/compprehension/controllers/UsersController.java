package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.UserInfoDto;

@Controller
@RequestMapping("api/users")
@Log4j2
@RequiredArgsConstructor
public class UsersController {
    private final UserFrontendService userService;

    @RequestMapping(value = { "whoami"}, method = { RequestMethod.GET })
    @ResponseBody
    public UserInfoDto getAll() throws Exception {
        return userService.getCurrentUserInfo();
    }

    private record SetLanguageRequest(String language) {}
    @RequestMapping(value = { "language"}, method = { RequestMethod.POST })
    @ResponseBody
    public String setLanguage(@RequestBody SetLanguageRequest language) throws Exception {
        userService.setLanguage(Language.fromString(language.language));
        return userService.getCurrentUserLanguage().toLocaleString();
    }
}
