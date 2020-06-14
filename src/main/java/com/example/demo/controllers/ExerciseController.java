package com.example.demo.controllers;

import com.example.demo.models.CourseModel;
import com.example.demo.models.ExerciseModel;
import com.example.demo.models.businesslogic.ExerciseForm;
import com.example.demo.models.businesslogic.FrontEndInfo;
import com.example.demo.models.entities.Exercise;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class ExerciseController {
    
    private ExerciseModel exerciseModel = new ExerciseModel();
    private CourseModel courseModel = new CourseModel();
    
    @GetMapping("/exercise/add")
    public String getExerciseForm(@RequestParam Long domain_id, 
                                  @RequestParam Long course_id, 
                                  @RequestParam Long user_id, Model model) {
               
        model.addAttribute("ExerciseForm", exerciseModel.
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
            exerciseModel.createExercise(filledForm, course_id, user_id, domain_id);        
        } catch (ExerciseModel.ExerciseFormException e) {
            model.addAttribute("filledForm", filledForm);
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("course", courseModel.getCourse(course_id));
            model.addAttribute("domain_id", domain_id);
            model.addAttribute("user_id", user_id);
            htmlTemplate = "exerciseForm";
            return htmlTemplate;
        }
        
        model.addAttribute("exercises", exerciseModel.getExercises(course_id));
        model.addAttribute("course", courseModel.getCourse(course_id));
        htmlTemplate = "exercisesEditPage";        
        model.addAttribute("user_id", user_id);
        
        return htmlTemplate;
    }


    @GetMapping("/exercise/edit")
    public String getExerciseFormToEdit(@RequestParam Long exercise_id, 
                                        @RequestParam Long user_id, 
                                        Model model) {

        model.addAttribute("ExerciseForm", exerciseModel.
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
            exerciseModel.saveExercise(filledForm, exercise_id, user_id);
        } catch (ExerciseModel.ExerciseFormException e) {
            model.addAttribute("filledForm", filledForm);
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("exercise_id", exercise_id);
            model.addAttribute("user_id", user_id);
            htmlTemplate = "exerciseForm";
            return htmlTemplate;
        }
        
        Exercise ex = exerciseModel.getExercise(exercise_id);        
        model.addAttribute("exercises", exerciseModel.getExercises(
                ex.getCourse().getId()));
        model.addAttribute("course", ex.getCourse());
        model.addAttribute("user_id", user_id);
        htmlTemplate = "exercisesEditPage";
        
        return htmlTemplate;
    }

    //TODO
    @GetMapping("/exercise/exercise_id")
    public String getExerciseQuestion(@RequestParam Long user_id, 
                                      @RequestParam Long exercise_id, 
                                      @RequestParam FrontEndInfo frontEndInfo, 
                                      Model model) {

        
        
        return "exerciseForm";
    }
}
