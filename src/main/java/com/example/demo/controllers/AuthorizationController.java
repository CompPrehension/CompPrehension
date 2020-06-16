package com.example.demo.controllers;

import com.example.demo.Service.UserActionService;
import com.example.demo.Service.UserService;
import com.example.demo.models.entities.EnumData.ActionType;
import com.example.demo.models.entities.User;
import com.example.demo.models.entities.UserAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.Date;


@Controller
public class AuthorizationController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserActionService userActionService;
    
    public final String [] LOGIN_ERRORS = { "Неверный логин или пароль" };
    
    @GetMapping("/login")
    public String checkUserData(@RequestParam String login, 
                                @RequestParam String password, Model model) {
        
        long userId = userService.checkUserData(login, password);
        if (userId == -1) {
            
            model.addAttribute("errors", LOGIN_ERRORS[0]);
            return "login";
        }
        
        model.addAttribute("user_id", userId);
        model.addAttribute("courses", userService.getUserCourses(userId));
        return "mainPage";
    }

    @GetMapping("/logout")
    public String logoutUser(@RequestParam Long user_id, Model model) {
        
        User user = userService.getUser(user_id);

        UserAction action = new UserAction();
        action.setActionType(ActionType.LOGOUT);
        action.setUser(user);
        action.setTime(new Date());
               
        user.getUserActions().add(action);
        
        userService.updateUserProfile(user);
        //userActionService.saveUserAction(action);
        
        return "login";
    }
    
    //TODO
    @GetMapping("/registration")
    public String getRegistrationForm(Model model) {
        
        return "registrationForm"; 
    }

    //TODO
    @PostMapping("/registration")
    public String registerUser() {
        
        return "login";
    }
    
    
}
