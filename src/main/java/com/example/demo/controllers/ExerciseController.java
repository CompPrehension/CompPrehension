package com.example.demo.controllers;

import com.example.demo.Exceptions.ExerciseFormException;
import com.example.demo.Exceptions.NotFoundEx.CourseNFException;
import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Exceptions.NotFoundEx.ExerciseNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.Service.CourseService;
import com.example.demo.Service.ExerciseService;
import com.example.demo.models.businesslogic.ExerciseForm;
import com.example.demo.models.businesslogic.frontend.QuestionFront;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("exercises")
public class ExerciseController {
    
    @Autowired
    private ExerciseService exerciseService;
    
    @Autowired
    private CourseService courseService;




    @GetMapping("{exercise_id}/question")
    public ResponseEntity<QuestionFront> getExerciseQuestion(@PathVariable Long exerciseId, 
                                                             @RequestParam Long userId) {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }
    
    
    
    @GetMapping("getExerciseForm")
    public ResponseEntity<ExerciseForm> getExerciseForm(@RequestParam String domain_id) {
          
        try {
            ExerciseForm exerciseFrom = exerciseService.getExerciseFrom(domain_id);
            return new ResponseEntity<>(exerciseFrom, HttpStatus.OK);
        } catch (DomainNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }        
    }

    @PostMapping("createExercise")
    public ResponseEntity<Map<String, String>> addExercise(@RequestParam ExerciseForm filledForm,
                                                           @RequestParam Long course_id,
                                                           @RequestParam Long user_id,
                                                           @RequestParam String domain_id) {
        try { 
            exerciseService.createExercise(filledForm, course_id, user_id, domain_id);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (ExerciseFormException e) {
            return new ResponseEntity<>(e.getErrors(), HttpStatus.FORBIDDEN);
        } catch (DomainNFException | CourseNFException | UserNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @GetMapping("{exerciseId}/edit")
    public ResponseEntity<ExerciseForm> getExerciseFormToEdit(@PathVariable Long exerciseId) {

        try {
            ExerciseForm exerciseFrom = exerciseService.getExerciseFormToEdit(exerciseId);
            return new ResponseEntity<>(exerciseFrom, HttpStatus.OK);
        } catch (DomainNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("{exerciseId}/edit")
    public ResponseEntity<Map<String, String>> editExercise(@RequestParam ExerciseForm filledForm,
                                          @RequestParam Long exercise_id,
                                          @RequestParam Long user_id) {
        
        try {
            exerciseService.updateExercise(filledForm, exercise_id, user_id);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (ExerciseFormException e) {
            return new ResponseEntity<>(e.getErrors(), HttpStatus.FORBIDDEN);
        } catch (ExerciseNFException | UserNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    
}
