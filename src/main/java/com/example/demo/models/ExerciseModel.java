package com.example.demo.models;

import com.example.demo.models.businesslogic.*;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.Domain;
import com.example.demo.models.entities.EnumData.Language;

import java.util.ArrayList;
import java.util.Map;

public class ExerciseModel {
/*
    private Core core = new Core();
    private Strategy strategy = new Strategy();
    
    private UserModel userModel = new UserModel();
    
    public ExerciseForm getExerciseFrom(long domainId) {

        return core.getDomain(domainId).getExerciseForm();
    }
    
    public ExerciseForm getExerciseFormToEdit(long exerciseId) {
        
        //Берем из базы упражнение по id
        Exercise exercise = new Exercise();
        ExerciseForm emptyForm = getExerciseFrom(exercise.getDomain().getId());
        emptyForm.fillForm(exercise);
        
        return emptyForm;
    }
    
    public ArrayList<Exercise> getExercises(long courseId){
    
        return new ArrayList<>();
    }
    
    public Exercise getExercise(long exercise_id) {
        return new Exercise();
    }
    
    public void createExercise(ExerciseForm filledForm, long courseId, long userId, long domainId) throws ExerciseFormException {

        checkErrors(filledForm);
        Exercise newExercise = core.getDomain(domainId).ProcessExerciseForm(filledForm);
        //Взять пользователя из базы
        //Взять пред. обл. из базы
        //Взять курс из базы        
        //newExercise.setAuthor(new User());        
        newExercise.setDomain(new Domain());
        newExercise.setCourse(new Course());
        core.saveExercise(newExercise);
    }
    
    public void saveExercise(ExerciseForm filledForm, long exerciseId, long userId) throws ExerciseFormException {
        
        checkErrors(filledForm);
        Exercise updatedExercise = new Exercise();  //Берем из базы exercise
        long domainId = updatedExercise.getDomain().getId();
        Exercise newExercise = core.getDomain(domainId).ProcessExerciseForm(filledForm);
        core.saveExercise(newExercise);
    }
    
    private void checkErrors(ExerciseForm filledForm) throws ExerciseFormException {
        
        Map<String, String> errors = filledForm.validate();

        if (errors != null) {
            ExerciseFormException ex = new ExerciseFormException("Форма заполнена с ошибками");
            ex.setErrors(errors);
            throw ex;
        }
    }
        
    public Question getExerciseQuestion(long userId, long exerciseId, FrontEndInfo frontEndInfo) {
        
        ExerciseAttempt exerciseAttempt = core.startExerciseAttempt(exerciseId, userId, frontEndInfo);
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        Language userLanguage = userModel.getUserLanguage(userId);
        com.example.demo.models.businesslogic.Domain domain = core.getDomain(getExercise(exerciseId).getDomain().getId());
        domain.makeQuestion(qr, userLanguage);
        return null;
    }


    public class ExerciseFormException extends Exception {

        private Map<String, String> errors;

        public ExerciseFormException(String message) {
            super(message);
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public void setErrors(Map<String, String> errors) {
            this.errors = errors;
        }
    }
*/
}
