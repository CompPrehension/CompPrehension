package com.example.demo.controllers;
import com.example.demo.Exceptions.NotFoundEx.CourseNFException;
import com.example.demo.Exceptions.NotFoundEx.UserCourseRoleNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.Service.CourseService;
import com.example.demo.Service.DomainService;
import com.example.demo.Service.ExerciseService;
import com.example.demo.Service.UserService;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.CourseRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("courses")
public class CourseController {
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private DomainService domainService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExerciseService exerciseService;
    
    @GetMapping("{courseId}")
    public ResponseEntity<Course> getCourse(@PathVariable Long courseId) {
        
        try {
            Course course = courseService.getCourse(courseId);
            return new ResponseEntity<>(course, HttpStatus.OK);
        } catch (CourseNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    
    
    @GetMapping("/user/{user_id}/course/{course_id}")
    public String chooseCourse(@PathVariable Long course_id,
                               @PathVariable Long user_id, Model model) {
        /*        
        String[] responseParams = {"exercises", "course", "user_id"};
        
        List<Exercise> exercises = courseService.getExercises(course_id);
        model.addAttribute(responseParams[0], exercises);
        
        Course course = courseService.getCourse(course_id);
        model.addAttribute(responseParams[1], course);
        
        CourseRole role = userService.getCourseRole(user_id, course_id);
        model.addAttribute(responseParams[2], user_id);
        return roleExerciseEditingPageMap.get(role);   */
        return null;
    }

    @GetMapping("{course_id}/students")
    public ResponseEntity<List<User>> getStudentsOnCourse(@PathVariable Long course_id) {

        try {
            List<User> students = courseService.getStudents(course_id);
            return new ResponseEntity<>(students, HttpStatus.OK);
        } catch (CourseNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    /*
    @GetMapping("/exercise/domains")
    public ResponseEntity<Iterable<Domain>> getDomains() {
        
        return ResponseEntity.ok().body(domainService.getDomains());
    }*/
    
    /*
    @GetMapping("add")
    public String getCourseCreatingPanel(@RequestParam Long user_id, Model model) {
        
        model.addAttribute("user_id", user_id);
        
        return "courseEditingPage";
    }*/

    @PostMapping("add")
    public ResponseEntity<Void> createCourse(@RequestParam Long authorId,
                                             @RequestParam Course course) {

        try {
            courseService.addCourse(course, authorId);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (UserCourseRoleNFException | UserNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    /*
    @GetMapping("/course/edit")
    public String getCourseCreatingPanel(@RequestParam Long course_id, 
                                         @RequestParam Long user_id, Model model) {

        model.addAttribute("course", courseService.getCourse(course_id));
        model.addAttribute("user_id", user_id);
        
        return "courseEditingPage";
    }*/

    @PostMapping("edit")
    public ResponseEntity<Void> updateCourse( @RequestBody Course course) {

        try {
            courseService.updateCourse(course);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (CourseNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }        
    }
    
    /*
    @GetMapping("/course/add_user")
    public String getUserAddingPanel(@RequestParam Long course_id,
                                     @RequestParam Long user_id, Model model) {

        model.addAttribute("course", courseService.getCourse(course_id));
        model.addAttribute("course_roles", courseService.getCourseRoles());
        model.addAttribute("user_id", user_id);
        
        return "userAddingPanel";
    }*/

    @PostMapping("add_user")
    public ResponseEntity<Void> getUserAddingPanel(@RequestParam String user_login, 
                                             @RequestParam CourseRole course_role,
                                             @RequestParam Long course_id) {
        
        try {
            User newCourseUser = userService.getUser(user_login);
            userService.addToCourse(newCourseUser.getId(), course_id, course_role);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (UserNFException | CourseNFException | UserCourseRoleNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
}
