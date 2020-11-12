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
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    
    @GetMapping("{courseId}/exercises")
    public ResponseEntity<List<Exercise>> getExercises(@PathVariable Long courseId) {

        try {
            Course course = courseService.getCourse(courseId);
            return new ResponseEntity<>(course.getExercises(), HttpStatus.OK);
        } catch (CourseNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
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
    
    @GetMapping("/exercise/domains")
    public ResponseEntity<Iterable<DomainEntity>> getDomains() {
        
        return ResponseEntity.ok().body(domainService.getDomainEntities());
    }
        
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
    
    @PostMapping("edit")
    public ResponseEntity<Void> updateCourse( @RequestBody Course course) {

        try {
            courseService.updateCourse(course);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (CourseNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }        
    }
        
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
