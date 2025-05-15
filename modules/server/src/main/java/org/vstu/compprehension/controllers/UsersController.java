package org.vstu.compprehension.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.UserInfoDto;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.Mapper;

@Controller
@RequestMapping("api/users")
@Log4j2
public class UsersController {
    private final UserService userService;

    @Autowired
    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = { "whoami"}, method = { RequestMethod.GET })
    @ResponseBody
    public UserInfoDto getAll() throws Exception {
        return Mapper.toDto(userService.getCurrentUser());
    }

    private record SetLanguageRequest(String language) {}
    @RequestMapping(value = { "language"}, method = { RequestMethod.POST })
    @ResponseBody
    public String setLanguage(@RequestBody SetLanguageRequest language) throws Exception {
        userService.setLanguage(Language.fromString(language.language));
        return userService.getCurrentUser().getPreferred_language().toLocaleString();
    }
}
