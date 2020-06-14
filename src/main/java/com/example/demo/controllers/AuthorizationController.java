package com.example.demo.controllers;

import com.example.demo.models.CourseModel;
import com.example.demo.models.UserModel;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;


@Controller
public class AuthorizationController {

    UserModel userModel = new UserModel();
    CourseModel courseModel = new CourseModel();
    
    public final String [] LOGIN_ERRORS = { "Неверный логин или пароль" };
    
    @GetMapping("/login")
    public String checkUserData(@RequestParam String login, 
                                @RequestParam String password, Model model) {
        
        long userId = userModel.checkUserData(login, password);
        if (userId == -1) {
            
            model.addAttribute("errors", LOGIN_ERRORS[0]);
            return "login";
        }
        
        model.addAttribute("user_id", userId);
        model.addAttribute("courses", userModel.getUserCourses(userId));
        return "mainPage";
    }

    @GetMapping("/logout")
    public String logoutUser(@RequestParam Long user_id, Model model) {
        
        
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
