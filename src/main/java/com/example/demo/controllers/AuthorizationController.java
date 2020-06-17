package com.example.demo.controllers;

import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.Service.UserActionService;
import com.example.demo.Service.UserService;
import com.example.demo.models.entities.EnumData.ActionType;
import com.example.demo.models.entities.User;
import com.example.demo.models.entities.UserAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.Date;


@RestController
public class AuthorizationController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserActionService userActionService;
    
    public final String [] LOGIN_ERRORS = { "Неверный логин или пароль" };
    
    @GetMapping("/login")
    public ResponseEntity<Long> checkUserData(@RequestParam String login,
                                        @RequestParam String password) {
        
        long userId = userService.checkUserData(login, password);
        if (userId == -1) {            
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(userId, HttpStatus.OK);
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logoutUser(@RequestParam Long user_id) {

        User user;
        try {
            user = userService.getUser(user_id);
        } catch (UserNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        UserAction action = new UserAction();
        action.setActionType(ActionType.LOGOUT);
        action.setUser(user);
        action.setTime(new Date());
               
        user.getUserActions().add(action);
        
        userService.updateUserProfile(user);
        //userActionService.saveUserAction(action);
        
        return new ResponseEntity<>(HttpStatus.OK);
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
