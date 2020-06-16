package com.example.demo.Service;

import com.example.demo.Exceptions.ExerciseFormException;
import com.example.demo.Exceptions.NotFoundEx.ExerciseNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.Dao.ExerciseDao;
import com.example.demo.models.businesslogic.*;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.ActionType;
import com.example.demo.models.entities.EnumData.AttemptStatus;
import com.example.demo.models.entities.EnumData.Language;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class ExerciseService {
    
    private ExerciseDao exerciseDao;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private DomainService domainService;
    
    @Autowired
    private UserActionService userActionService;

    @Autowired
    private UserActionExerciseService userActionExerciseService;

    @Autowired
    private ExerciseAttemptService exerciseAttemptService;
    
    @Autowired
    private QuestionAttemptService questionAttemptService;

    @Autowired
    private QuestionService questionService;

    private Core core = new Core();
    
    private Strategy strategy = new Strategy();
    
    
    @Autowired
    public ExerciseService(ExerciseDao exerciseDao) {
        this.exerciseDao = exerciseDao;
    }

   

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

    public Exercise getExercise(long exerciseId) {
        try {
            return exerciseDao.findExerciseById(exerciseId).orElseThrow(()->
                    new ExerciseNFException("Exercise with id: " + exerciseId + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-exercise to Model-exercise", e);
        }
    }

    
    public void createExercise(ExerciseForm filledForm, long courseId, long userId, long domainId) throws ExerciseFormException {

        checkErrors(filledForm);
        Exercise newExercise = core.getDomain(domainId).ProcessExerciseForm(filledForm);
        
        //Создаем доп. таблицы в связи с созданием упражнения
        User user = userService.getUser(userId);

        UserAction action = new UserAction();
        action.setActionType(ActionType.CREATE_EXERCISE);
        action.setUser(user);
        action.setTime(new Date());

        UserActionExercise userActionExercise = new UserActionExercise();
        userActionExercise.setExercise(newExercise);
        userActionExercise.setUserAction(action);
        newExercise.getUserActionExercises().add(userActionExercise);
        newExercise.setDomain(domainService.getDomain(domainId));
        newExercise.setCourse(courseService.getCourse(courseId));
        
        action.setUserActionExercise(userActionExercise);
        
        user.getUserActions().add(action);

        userService.updateUserProfile(user);
        userActionService.saveUserAction(action);
        //userActionExerciseService.saveUserActionExercise(userActionExercise);
        exerciseDao.save(newExercise);
        
        //core.saveExercise(newExercise);
    }

    public void updateExercise(ExerciseForm filledForm, long exerciseId, long userId) throws ExerciseFormException {

        checkErrors(filledForm);
        
        Exercise updatedExercise = getExercise(exerciseId);  //Берем из базы exercise
        long domainId = updatedExercise.getDomain().getId();
        Exercise newExercise = core.getDomain(domainId).ProcessExerciseForm(filledForm);
        newExercise.setId(exerciseId);
        //Создаем доп. таблицы в связи с созданием упражнения
        User user = userService.getUser(userId);

        UserAction action = new UserAction();
        action.setActionType(ActionType.EDIT_EXERCISE);
        action.setUser(user);
        action.setTime(new Date());

        UserActionExercise userActionExercise = new UserActionExercise();
        userActionExercise.setExercise(newExercise);
        userActionExercise.setUserAction(action);
        newExercise.getUserActionExercises().add(userActionExercise);

        action.setUserActionExercise(userActionExercise);

        user.getUserActions().add(action);

        userService.updateUserProfile(user);
        userActionService.saveUserAction(action);
        //userActionExerciseService.saveUserActionExercise(userActionExercise);
        exerciseDao.save(newExercise);
        
        //core.saveExercise(newExercise);
    }

    private void checkErrors(ExerciseForm filledForm) throws ExerciseFormException {

        Map<String, String> errors = filledForm.validate();

        if (errors != null) {
            ExerciseFormException ex = new ExerciseFormException("Форма заполнена с ошибками");
            ex.setErrors(errors);
            throw ex;
        }
    }

    public Question getFirstExerciseQuestion(long userId, long exerciseId, FrontEndInfo frontEndInfo) {

        //ExerciseAttempt exerciseAttempt = core.startExerciseAttempt(exerciseId, userId, frontEndInfo);
        //Создаем попытку выполнения упражнения
        ExerciseAttempt exerciseAttempt = new ExerciseAttempt();
        exerciseAttempt.setAttemptStatus(AttemptStatus.INCOMPLETE);
        exerciseAttempt.setExercise(getExercise(exerciseId));
        exerciseAttempt.setUser(userService.getUser(userId));
        
        //Создаем попытку выполнения вопроса
        QuestionAttempt questionAttempt = new QuestionAttempt();
        questionAttempt.setExerciseAttempt(exerciseAttempt);

        //Генерируем вопрос        
        Question newQuestion = questionService.generateBusinessLogicQuestion(
                exerciseAttempt);
        
        questionAttempt.setQuestion(newQuestion.getQuestionData());
        exerciseAttempt.getQuestionAttempts().add(questionAttempt);
        
        //Сохраняем получившиеся попытки в базу (вместе с этим сохраняется и вопрос)
        exerciseAttemptService.saveExerciseAttempt(exerciseAttempt);
        questionAttemptService.saveQuestionAttempt(questionAttempt);
        
        return newQuestion;
    }

}
