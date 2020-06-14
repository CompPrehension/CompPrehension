package com.example.demo.controllers;

import com.example.demo.models.CourseModel;
import com.example.demo.models.UserModel;
import com.example.demo.models.entities.Course;
import com.example.demo.models.entities.EnumData.CourseRole;
import com.example.demo.models.entities.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class UserController {
    
    private UserModel userModel = new UserModel();
    private CourseModel courseModel = new CourseModel();
    
    @GetMapping("/course/{course_id}/students")
    public String getStudentsOnCourse(@PathVariable Long course_id, 
                                      @RequestParam Long user_id, Model model) {
                
        model.addAttribute("students", userModel.getStudents(course_id));
        model.addAttribute("course", courseModel.getCourse(course_id));
        model.addAttribute("user_id", user_id);
        
        return "students";
    }

    @GetMapping("/student/{student_id}/actions")
    public String getStudentActions(@PathVariable Long student_id, 
                                    @RequestParam Long user_id, Model model) {

        model.addAttribute("actions", userModel.getStudents(student_id));
        model.addAttribute("user_id", user_id);

        return "students";
    }
    
    @GetMapping("/user_full_info/{user_profile_id}")
    public String getUserInfo(@PathVariable Long user_profile_id, 
                              @RequestParam Long user_id, Model model) {
        
        User userProfile = userModel.getUser(user_profile_id);
        ArrayList<Course> userCourses = userModel.getUserCourses(user_profile_id);
        HashMap <Course, CourseRole> courseRoles = new HashMap<>();
        for (Course c : userCourses) {
            courseRoles.put(c, userModel.getCourseRole(user_profile_id, c.getId()));
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
                                      @RequestParam Map<Course, CourseRole> courseRoles, @RequestParam Long user_id) {

       userModel.updateUserProfile(userProfile);        
        ArrayList<Course> userCourses = new ArrayList(Arrays.asList(courseRoles.keySet().toArray()));
        userModel.setUserCourses(user_profile_id, userCourses);
        for (Course c : userCourses) {
            userModel.setCourseRole(user_profile_id, c.getId(), courseRoles.get(c));
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/user_short_info")
    public ResponseEntity getShortStudentInfo(@RequestParam Long login, 
                                              @RequestParam Long user_id, 
                                              @RequestParam Long course_id,
                                              Model model) {

        User userInfo = userModel.getUser(login);
        model.addAttribute("user", userInfo);
        model.addAttribute("groups", userModel.getUserGroups(userInfo.getId()));
        model.addAttribute("is_user_singed_up", userModel.getUserCourses(user_id).
                contains(courseModel.getCourse(course_id)));
        
        return ResponseEntity.ok().build();
    }    
}
