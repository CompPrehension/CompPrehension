package org.vstu.compprehension.controllers;
import org.vstu.compprehension.Exceptions.NotFoundEx.CourseNFException;
import org.vstu.compprehension.Exceptions.NotFoundEx.UserCourseRoleNFException;
import org.vstu.compprehension.Exceptions.NotFoundEx.UserNFException;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.DomainService;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.CourseEntity;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.EnumData.CourseRole;
import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.models.entities.UserEntity;

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
    public ResponseEntity<CourseEntity> getCourse(@PathVariable Long courseId) {
        
        try {
            CourseEntity course = courseService.getCourse(courseId);
            return new ResponseEntity<>(course, HttpStatus.OK);
        } catch (CourseNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("{courseId}/exercises")
    public ResponseEntity<List<ExerciseEntity>> getExercises(@PathVariable Long courseId) {

        try {
            CourseEntity course = courseService.getCourse(courseId);
            return new ResponseEntity<>(course.getExercises(), HttpStatus.OK);
        } catch (CourseNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    
    @GetMapping("{course_id}/students")
    public ResponseEntity<List<UserEntity>> getStudentsOnCourse(@PathVariable Long course_id) {

        try {
            List<UserEntity> students = courseService.getStudents(course_id);
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
                                             @RequestParam CourseEntity course) {

        try {
            courseService.addCourse(course, authorId);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (UserCourseRoleNFException | UserNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PostMapping("edit")
    public ResponseEntity<Void> updateCourse( @RequestBody CourseEntity course) {

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
            UserEntity newCourseUser = userService.getUser(user_login);
            userService.addToCourse(newCourseUser.getId(), course_id, course_role);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (UserNFException | CourseNFException | UserCourseRoleNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
}
