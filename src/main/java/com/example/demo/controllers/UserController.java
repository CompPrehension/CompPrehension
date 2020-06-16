package com.example.demo.controllers;

import com.example.demo.Service.CourseService;
import com.example.demo.Service.UserService;
import com.example.demo.models.CourseModel;
import com.example.demo.models.UserModel;
import com.example.demo.models.entities.Action;
import com.example.demo.models.entities.Course;
import com.example.demo.models.entities.EnumData.CourseRole;
import com.example.demo.models.entities.User;
import com.example.demo.models.entities.UserAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;
    
    @GetMapping("/course/{course_id}/students")
    public String getStudentsOnCourse(@PathVariable Long course_id, 
                                      @RequestParam Long user_id, Model model) {
                
        model.addAttribute("students", courseService.getStudents(course_id));
        model.addAttribute("course", courseService.getCourse(course_id));
        model.addAttribute("user_id", user_id);
        
        return "students";
    }

    @GetMapping("/student/{student_id}/actions")
    public String getStudentActions(@PathVariable Long student_id, 
                                    @RequestParam Long user_id, Model model) {
        
        List<Action> actionsToFront = new ArrayList<>();
        List<UserAction> userActions = userService.getUserActions(student_id);
        User user = userService.getUser(student_id);
        for (UserAction ua : userActions) {
            Action tmp = new Action();
            tmp.setUserName(user.getFirstName() + user.getLastName());
            tmp.setActionType(ua.getActionType().toString());
            tmp.setActionTime(ua.getTime());
            tmp.setExerciseName(ua.getUserActionExercise().getExercise().getName());
            actionsToFront.add(tmp);
        }
        
        model.addAttribute("actions", actionsToFront);
        model.addAttribute("user_id", user_id);

        return "students";
    }
    
    @GetMapping("/user_full_info/{user_profile_id}")
    public String getUserInfo(@PathVariable Long user_profile_id, 
                              @RequestParam Long user_id, Model model) {
        
        User userProfile = userService.getUser(user_profile_id);
        List<Course> userCourses = userService.getUserCourses(user_profile_id);
        Map <Course, CourseRole> courseRoles = new HashMap<>();
        for (Course c : userCourses) {
            courseRoles.put(c, userService.getCourseRole(user_profile_id, c.getId()));
        }
        
        model.addAttribute("userProfile", userProfile);
        model.addAttribute("courseRoles", courseRoles);
        model.addAttribute("user_id", user_id);
        
        if (user_profile_id == user_id) {
            return "userDataEditingPage";
        }
        
        return "userInfo";
    }

    @PostMapping("/user_full_info/{user_profile_id}")
    public ResponseEntity setUserInfo(@PathVariable Long user_profile_id, 
                                      @RequestParam User userProfile, 
                                      @RequestParam Map<Course, CourseRole> courseRoles, 
                                      @RequestParam Long user_id) {

       userService.updateUserProfile(userProfile);        
        ArrayList<Course> userCourses = new ArrayList(Arrays.asList(
                courseRoles.keySet().toArray()));
        userService.setUserCourses(user_profile_id, userCourses);
        for (Course c : userCourses) {
            userService.setCourseRole(user_profile_id, c.getId(), courseRoles.get(c));
        }

        return ResponseEntity.ok().build();
    }

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
