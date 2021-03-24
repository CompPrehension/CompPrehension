package org.vstu.compprehension.controllers;

import org.vstu.compprehension.Exceptions.NotFoundEx.UserActionNFException;
import org.vstu.compprehension.Exceptions.NotFoundEx.UserNFException;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.Action;
import org.vstu.compprehension.models.entities.CourseEntity;
import org.vstu.compprehension.models.entities.UserCourseRoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.models.entities.UserEntity;

import java.util.*;

@RestController
@RequestMapping("users")
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @GetMapping("{userId}/courses")
    public ResponseEntity<List<CourseEntity>> getUserCourses(@PathVariable Long userId) {
        List<CourseEntity> courses = new ArrayList<>();
        try {
            UserEntity user = userService.getUser(userId);
            List<UserCourseRoleEntity> userCourseRoles = user.getUserCourseRoles();
            for (UserCourseRoleEntity ucr : userCourseRoles) {
                
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
    public ResponseEntity<UserEntity> getUserInfo(@PathVariable Long user_profile_id) {
        
        try {
            UserEntity userProfile = userService.getUser(user_profile_id);
            return new ResponseEntity<>(userProfile, HttpStatus.OK);
        } catch (UserNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }        
    }

    @PostMapping("update")
    public ResponseEntity<Void> setUserInfo(@RequestBody UserEntity user) {

       userService.updateUserProfile(user); 
        return ResponseEntity.ok().build();
    }

    //TODO
    @GetMapping("/user_short_info")
    public ResponseEntity getShortStudentInfo(@RequestParam Long login, 
                                              @RequestParam Long user_id, 
                                              @RequestParam Long course_id,
                                              Model model) {

        UserEntity userInfo = userService.getUser(login);
        model.addAttribute("user", userInfo);
        model.addAttribute("groups", userService.getUserGroups(userInfo.getId()));
        model.addAttribute("is_user_singed_up", userService.getUserCourses(user_id).
                contains(courseService.getCourse(course_id)));
        
        return ResponseEntity.ok().build();
    }    
}
