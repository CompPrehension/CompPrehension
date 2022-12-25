package org.vstu.compprehension.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.dto.UserInfoDto;
import org.vstu.compprehension.models.businesslogic.user.UserContext;
import org.vstu.compprehension.utils.Mapper;

import java.util.List;

@Controller
@RequestMapping("api/users")
@Log4j2
public class UsersController {
    private final UserContext userContext;

    @Autowired
    public UsersController(UserContext userContext) {
        this.userContext = userContext;
    }

    @RequestMapping(value = { "whoami"}, method = { RequestMethod.GET })
    @ResponseBody
    public UserInfoDto getAll() {
        return Mapper.toDto(userContext);
    }
}
