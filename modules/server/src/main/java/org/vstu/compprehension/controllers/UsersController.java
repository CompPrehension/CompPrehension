package org.vstu.compprehension.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.ExercisePermissionService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.UserInfoDto;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.utils.Mapper;

@Controller
@RequestMapping("api/users")
@Log4j2
public class UsersController {
    private final UserService userService;
    private final ExercisePermissionService exercisePermissionService;

    @Autowired
    public UsersController(UserService userService, ExercisePermissionService exercisePermissionService) {
        this.userService = userService;
        this.exercisePermissionService = exercisePermissionService;
    }

    @RequestMapping(value = { "whoami"}, method = { RequestMethod.GET })
    @ResponseBody
    public UserInfoDto getAll() throws Exception {
        UserEntity user = userService.getCurrentUser();
        return Mapper.toDto(user, exercisePermissionService.ofUser(user.getId()));
    }

    private record SetLanguageRequest(String language) {}
    @RequestMapping(value = { "language"}, method = { RequestMethod.POST })
    @ResponseBody
    public String setLanguage(@RequestBody SetLanguageRequest language) throws Exception {
        userService.setLanguage(Language.fromString(language.language));
        return userService.getCurrentUser().getPreferred_language().toLocaleString();
    }
}
