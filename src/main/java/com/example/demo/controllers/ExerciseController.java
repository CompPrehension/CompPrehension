package com.example.demo.controllers;

import com.example.demo.Exceptions.ExerciseFormException;
import com.example.demo.Exceptions.NotFoundEx.CourseNFException;
import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Exceptions.NotFoundEx.ExerciseNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.Service.CourseService;
import com.example.demo.Service.ExerciseService;
import com.example.demo.models.businesslogic.ExerciseForm;
import com.example.demo.models.businesslogic.FrontEndInfo;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.QuestionAttempt;
import com.example.demo.models.entities.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResponseExtractor;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("exercises")
public class ExerciseController {
    
    @Autowired
    private ExerciseService exerciseService;
    
    @Autowired
    private CourseService courseService;
    
    
    
    @GetMapping("getExerciseForm")
    public ResponseEntity<ExerciseForm> getExerciseForm(@RequestParam Long domain_id) {
          
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
                                                           @RequestParam Long domain_id) {
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

    
    @GetMapping("/exercise/{exercise_id}")
    public String getExerciseQuestion(@PathVariable Long exercise_id,
                                      @RequestParam Long user_id,                                       
                                      @RequestParam FrontEndInfo frontEndInfo, 
                                      Model model) {
        
        Question question = exerciseService.getFirstExerciseQuestion(user_id,
                exercise_id, frontEndInfo);
        List<QuestionAttempt> attempts = question.getQuestionData().
                getQuestionAttempts();
        //К этому моменту уже будет минимум 1 попытка
        model.addAttribute("questionAttempt_id", attempts.get(attempts.size() - 1).getId());  
        model.addAttribute("questionFront", question);
        
        return "exerciseForm";
    }
    
    
}
