package com.example.demo.controllers;

import com.example.demo.Exceptions.ExerciseFormException;
import com.example.demo.Service.CourseService;
import com.example.demo.Service.ExerciseService;
import com.example.demo.models.businesslogic.ExerciseForm;
import com.example.demo.models.businesslogic.FrontEndInfo;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.QuestionAttempt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Controller
public class ExerciseController {
    
    @Autowired
    private ExerciseService exerciseService;
    
    @Autowired
    private CourseService courseService;
    
    
    
    @GetMapping("/exercise/add")
    public String getExerciseForm(@RequestParam Long domain_id, 
                                  @RequestParam Long course_id, 
                                  @RequestParam Long user_id, Model model) {
               
        model.addAttribute("ExerciseForm", exerciseService.
                getExerciseFrom(domain_id));
        model.addAttribute("course_id", course_id);
        model.addAttribute("user_id", user_id);
        model.addAttribute("domain_id", domain_id);
        
        return "exerciseForm";
    }

    @PostMapping("/exercise/add")
    public String addExercise(@RequestParam ExerciseForm filledForm, 
                              @RequestParam Long course_id, 
                              @RequestParam Long user_id, 
                              @RequestParam Long domain_id, Model model) {

        String htmlTemplate = "";

        try { 
            exerciseService.createExercise(filledForm, course_id, user_id, domain_id);        
        } catch (ExerciseFormException e) {
            model.addAttribute("filledForm", filledForm);
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("course", courseService.getCourse(course_id));
            model.addAttribute("domain_id", domain_id);
            model.addAttribute("user_id", user_id);
            htmlTemplate = "exerciseForm";
            return htmlTemplate;
        }
        
        model.addAttribute("exercises", courseService.getExercises(course_id));
        model.addAttribute("course", courseService.getCourse(course_id));
        htmlTemplate = "exercisesEditPage";        
        model.addAttribute("user_id", user_id);
        
        return htmlTemplate;
    }


    @GetMapping("/exercise/edit")
    public String getExerciseFormToEdit(@RequestParam Long exercise_id, 
                                        @RequestParam Long user_id, 
                                        Model model) {

        model.addAttribute("ExerciseForm", exerciseService.
                getExerciseFormToEdit(exercise_id));
        model.addAttribute("exercise_id", exercise_id);
        model.addAttribute("user_id", user_id);

        return "exerciseForm";
    }

    @PostMapping("/exercise/edit")
    public String editExercise(@RequestParam ExerciseForm filledForm, 
                               @RequestParam Long exercise_id,
                              @RequestParam Long user_id, Model model) {

        String htmlTemplate = "";

        try {
            exerciseService.updateExercise(filledForm, exercise_id, user_id);
        } catch (ExerciseFormException e) {
            model.addAttribute("filledForm", filledForm);
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("exercise_id", exercise_id);
            model.addAttribute("user_id", user_id);
            htmlTemplate = "exerciseForm";
            return htmlTemplate;
        }
        
        Exercise ex = exerciseService.getExercise(exercise_id);        
        model.addAttribute("exercises", courseService.getExercises(
                ex.getCourse().getId()));
        model.addAttribute("course", ex.getCourse());
        model.addAttribute("user_id", user_id);
        htmlTemplate = "exercisesEditPage";
        
        return htmlTemplate;
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
