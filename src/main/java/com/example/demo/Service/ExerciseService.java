package com.example.demo.Service;

import com.example.demo.Exceptions.ExerciseFormException;
import com.example.demo.Exceptions.NotFoundEx.ExerciseNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.repository.ExerciseRepository;
import com.example.demo.models.businesslogic.*;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.ActionType;
import com.example.demo.models.entities.EnumData.AttemptStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExerciseService {

    private ExerciseRepository exerciseRepository;

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

    @Autowired
    private Strategy strategy;

    @Autowired
    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    public ExerciseForm getExerciseFrom(String domainId) {

        return core.getDomain(domainId).getExerciseForm();
    }

    public ExerciseForm getExerciseFormToEdit(long exerciseId) {

        //Берем из базы упражнение по id
        ExerciseEntity exercise = new ExerciseEntity();
        ExerciseForm emptyForm = getExerciseFrom(exercise.getDomain().getName());
        emptyForm.fillForm(exercise);

        return emptyForm;
    }

    public List<Tag> getTags(ExerciseEntity exercise) {
        String[] tags = exercise.getTags().split(",");
        List<Tag> result = new ArrayList<>();
        for (String tagString : tags) {
            Tag tag = new Tag();
            tag.setName(tagString);
            result.add(tag);
        }
        for (String tagString : List.of("basics", "operators", "order", "evaluation")) {
            Tag tag = new Tag();
            tag.setName(tagString);
            result.add(tag);
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    public ExerciseEntity getExercise(long exerciseId) {
        try {
            return exerciseRepository.findById(exerciseId).orElseThrow(()->
                    new ExerciseNFException("Exercise with id: " + exerciseId + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-exercise to Model-exercise", e);
        }
    }

    public void createExercise(ExerciseForm filledForm, long courseId, long userId, String domainId) throws ExerciseFormException {

        checkErrors(filledForm);
        ExerciseEntity newExercise = core.getDomain(domainId).processExerciseForm(filledForm);

        //Создаем доп. таблицы в связи с созданием упражнения
        UserEntity user = userService.getUser(userId);

        UserActionEntity action = new UserActionEntity();
        action.setActionType(ActionType.CREATE_EXERCISE);
        action.setUser(user);
        action.setTime(new Date());

        UserActionExerciseEntity userActionExercise = new UserActionExerciseEntity();
        userActionExercise.setExercise(newExercise);
        userActionExercise.setUserAction(action);
        newExercise.getUserActionExercises().add(userActionExercise);
        newExercise.setDomain(domainService.getDomainEntity(domainId));
        newExercise.setCourse(courseService.getCourse(courseId));

        action.setUserActionExercise(userActionExercise);

        user.getUserActions().add(action);

        userService.updateUserProfile(user);
        userActionService.saveUserAction(action);
        //userActionExerciseService.saveUserActionExercise(userActionExercise);
        exerciseRepository.save(newExercise);

        //core.saveExercise(newExercise);
    }

    public void updateExercise(ExerciseForm filledForm, long exerciseId, long userId) throws ExerciseFormException {

        checkErrors(filledForm);

        ExerciseEntity updatedExercise = getExercise(exerciseId);  //Берем из базы exercise
        String domainId = updatedExercise.getDomain().getName();
        ExerciseEntity newExercise = core.getDomain(domainId).processExerciseForm(filledForm);
        newExercise.setId(exerciseId);
        //Создаем доп. таблицы в связи с созданием упражнения
        UserEntity user = userService.getUser(userId);

        UserActionEntity action = new UserActionEntity();
        action.setActionType(ActionType.EDIT_EXERCISE);
        action.setUser(user);
        action.setTime(new Date());

        UserActionExerciseEntity userActionExercise = new UserActionExerciseEntity();
        userActionExercise.setExercise(newExercise);
        userActionExercise.setUserAction(action);
        newExercise.getUserActionExercises().add(userActionExercise);

        action.setUserActionExercise(userActionExercise);

        user.getUserActions().add(action);

        userService.updateUserProfile(user);
        userActionService.saveUserAction(action);
        //userActionExerciseService.saveUserActionExercise(userActionExercise);
        exerciseRepository.save(newExercise);

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

    public Question getExerciseQuestion(long userId, long exerciseId, FrontEndInfo frontEndInfo) {

        //ExerciseAttempt exerciseAttempt = core.startExerciseAttempt(exerciseId, userId, frontEndInfo);
        //Создаем попытку выполнения упражнения
        ExerciseAttemptEntity exerciseAttempt = new ExerciseAttemptEntity();
        exerciseAttempt.setAttemptStatus(AttemptStatus.INCOMPLETE);
        exerciseAttempt.setExercise(getExercise(exerciseId));
        exerciseAttempt.setUser(userService.getUser(userId));

        //Создаем попытку выполнения вопроса
        QuestionAttemptEntity questionAttempt = new QuestionAttemptEntity();
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
