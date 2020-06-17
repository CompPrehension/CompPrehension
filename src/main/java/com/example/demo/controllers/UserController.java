package com.example.demo.controllers;

import com.example.demo.Exceptions.NotFoundEx.UserActionNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.Service.CourseService;
import com.example.demo.Service.UserService;
import com.example.demo.models.CourseModel;
import com.example.demo.models.UserModel;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.CourseRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("users")
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @GetMapping("{userId}/courses")
    public ResponseEntity<List<Course>> getUserCourses(@PathVariable Long userId) {
        List<Course> courses = new ArrayList<>(); 
        try {
            User user = userService.getUser(userId);
            List<UserCourseRole> userCourseRoles = user.getUserCourseRoles();
            for (UserCourseRole ucr : userCourseRoles) {
                
                courses.add(ucr.getCourse());
            }
        } catch (UserNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(courses, HttpStatus.OK);
    }
    

    @GetMapping("{userId}/actions")
    public ResponseEntity<List<Action>> getUserActions(@PathVariable Long userId) {
        
        try {
            List<Action> actionsToFront = userService.getUserActionsToFront(userId);
            return new ResponseEntity<>(actionsToFront, HttpStatus.OK);
        } catch (UserNFException | UserActionNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("{userId}")
    public ResponseEntity<User> getUserInfo(@PathVariable Long user_profile_id) {
        
        try {
            User userProfile = userService.getUser(user_profile_id);
            return new ResponseEntity<>(userProfile, HttpStatus.OK);
        } catch (UserNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }        
    }

    @PostMapping("update")
    public ResponseEntity<Void> setUserInfo(@RequestBody User user) {

       userService.updateUserProfile(user); 
        return ResponseEntity.ok().build();
    }

    //TODO
    @GetMapping("/user_short_info")
    public ResponseEntity getShortStudentInfo(@RequestParam Long login, 
                                              @RequestParam Long user_id, 
                                              @RequestParam Long course_id,
                                              Model model) {

        User userInfo = userService.getUser(login);
        model.addAttribute("user", userInfo);
        model.addAttribute("groups", userService.getUserGroups(userInfo.getId()));
        model.addAttribute("is_user_singed_up", userService.getUserCourses(user_id).
                contains(courseService.getCourse(course_id)));
        
        return ResponseEntity.ok().build();
    }    
}
