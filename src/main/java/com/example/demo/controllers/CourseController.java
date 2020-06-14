package com.example.demo.controllers;
import com.example.demo.models.*;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.CourseRole;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Controller
public class CourseController {
    
    private Map<CourseRole, String> roleExerciseEditingPageMap = new HashMap<CourseRole, String>() {{
        put(CourseRole.STUDENT, "exercisesPage");
        put(CourseRole.SUPERVISOR, "exercisesPage");
        put(CourseRole.TEACHER, "exercisesEditPage");
    }};

    private CourseModel courseModel = new CourseModel();
    private DomainModel domainModel = new DomainModel();
    private UserModel userModel = new UserModel();
    private ExerciseModel exerciseModel = new ExerciseModel();
    
    
    @GetMapping("/user/{user_id}/course/{course_id}")
    public String chooseCourse(@PathVariable Long course_id,
                               @PathVariable Long user_id, Model model) {
                
        String[] responseParams = {"exercises", "course", "user_id"};
        
        ArrayList<Exercise> exercises = exerciseModel.getExercises(course_id);
        model.addAttribute(responseParams[0], exercises);
        
        Course course = courseModel.getCourse(course_id);
        model.addAttribute(responseParams[1], course);
        
        CourseRole role = userModel.getCourseRole(user_id, course_id);
        model.addAttribute(responseParams[2], user_id);
        return roleExerciseEditingPageMap.get(role);        
    }

    @GetMapping("/exercise/domains")
    public ResponseEntity<Iterable<Domain>> getDomains() {
        
        return ResponseEntity.ok().body(domainModel.getDomains());
    }
    
    
    @GetMapping("/course/add")
    public String getCourseCreatingPanel(@RequestParam Long user_id, Model model) {
        
        model.addAttribute("user_id", user_id);
        
        return "courseEditingPage";
    }

    @PostMapping("/course/add")
    public String createCourse(@RequestParam String course_name, 
                               @RequestParam String description, 
                               @RequestParam Long user_id, 
                               Model model) {

        courseModel.addCourse(course_name, description, user_id);
        
        model.addAttribute("courses", userModel.getUserCourses(user_id));
        model.addAttribute("user_id", user_id);

        return "mainPage";
    }
    
    @GetMapping("/course/edit")
    public String getCourseCreatingPanel(@RequestParam Long course_id, 
                                         @RequestParam Long user_id, Model model) {

        model.addAttribute("course", courseModel.getCourse(course_id));
        model.addAttribute("user_id", user_id);
        
        return "courseEditingPage";
    }

    @PostMapping("/course/edit")
    public String updateCourse( @RequestParam Course course,
                                @RequestParam Long user_id, Model model) {

        courseModel.updateCourse(course);

        model.addAttribute("courses", userModel.getUserCourses(user_id));
        model.addAttribute("user_id", user_id);
        
        return "mainPage";
    }
    
    @GetMapping("/course/add_user")
    public String getUserAddingPanel(@RequestParam Long course_id,
                                     @RequestParam Long user_id, Model model) {

        model.addAttribute("course", courseModel.getCourse(course_id));
        model.addAttribute("course_roles", courseModel.getCourseRoles());
        model.addAttribute("user_id", user_id);
        
        return "userAddingPanel";
    }

    @PostMapping("/course/add_user")
    public ResponseEntity getUserAddingPanel(@RequestParam String user_login, 
                                             @RequestParam CourseRole course_role,
                                             @RequestParam Long course_id, 
                                             @RequestParam Long user_id, 
                                             Model model) {
        
        User newCourseUser = userModel.getUser(user_login);
        userModel.addToCourse(newCourseUser.getId(), course_id, course_role);
        
        return ResponseEntity.ok().build();
    }
    
}
