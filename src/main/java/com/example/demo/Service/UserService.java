package com.example.demo.Service;

import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.CourseRole;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.Role;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private UserRepository userRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserCourseRoleService userCourseRoleService;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity getUserByEmail(String email){
        try{
            return userRepository.findUserByEmail(email).orElseThrow(()->
                    new UserNFException("User with email: " + email + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }
    
    public UserEntity getUserByLogin(String login) {
        try{
            return userRepository.findUserByLogin(login).orElseThrow(()->
                    new UserNFException("User with login: " + login + "Not Found"));
        } catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }

    public long checkUserData(String login, String password) {
        
        UserEntity user;
        try { user = getUser(login); } 
        catch (UserNFException e) { return -1; }
        //Если пользователь не найден или у найденного пользователя не тот пароль
        if (user == null || !user.getPassword().equals(password)) { return -1; }

        return user.getId();
    }

    public UserEntity getUser(long userId) {
        try {  
            return userRepository.findById(userId).orElseThrow(()->
                    new UserNFException("User with id: " + userId + "Not Found"));
        } catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }

    public UserEntity getUser(String login) {
        try {
            return userRepository.findUserByLogin(login).orElseThrow(()->
                    new UserNFException("User with login: " + login + "Not Found"));
        } catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }


    /**
     * Удалить пользователя с курса
     * @param userId - id пользователя, которого будем удалять с курса
     * @param courseId - id курса, с которого будем удалять пользователя
     */
    public void removeFromCourse(long userId, long courseId) {

    }


    /**
     * Добавить пользователя на курс (по умолчанию добавляется пользователь
     * с ролью - студент)
     * @param userId - id пользователя, которого будем добавлять
     * @param courseId - id курса, куда будем добавлять пользователя
     */
    public void addToCourse(long userId, long courseId) {

    }

    /**
     * Добавить пользователя на курс
     * @param userId - id пользователя, которого будем добавлять
     * @param courseId - id курса, куда будем добавлять пользователя
     * @param role - роль пользователя в рамках курса
     */
    public void addToCourse(long userId, long courseId, CourseRole role) {

        UserEntity user = getUser(userId);
        CourseEntity course = courseService.getCourse(courseId);
        UserCourseRoleEntity userCourseRole = new UserCourseRoleEntity();
        userCourseRole.setUser(user);
        userCourseRole.setCourseRole(role);
        userCourseRole.setCourse(course);

        course.getUserCourseRoles().add(userCourseRole);
        user.getUserCourseRoles().add(userCourseRole);
        
        courseService.updateCourse(course);
        userRepository.save(user);
        userCourseRoleService.saveUserCourseRole(userCourseRole);
    }


    /**
     * @param userId - id пользователя, действия которого хотим получить
     * @return - список действий пользователя
     */
    public List<UserActionEntity> getUserActions(long userId) {

        return getUser(userId).getUserActions();
    }

    public List<Action> getUserActionsToFront(long userId) {
        
        List<Action> actions = new ArrayList<>();
        UserEntity user = getUser(userId);
        for (UserActionEntity ua : getUserActions(userId)) {
            Action tmp = new Action();
            tmp.setUserName(user.getFirstName() + user.getLastName());
            tmp.setActionType(ua.getActionType().toString());
            tmp.setActionTime(ua.getTime());
            if (ua.getUserActionExercise() != null) {
                tmp.setExerciseName(ua.getUserActionExercise().getExercise().getName());
            }            
            actions.add(tmp);
        }
        
        return actions;
    }

    public void saveUser(UserEntity user) {
        try {
            if (getUser(user.getLogin()) != null) {
                throw new DataIntegrityViolationException("Пользователь с таким логином " +
                        "уже существует");
            }
        } catch (UserNFException e) {
            userRepository.save(user);
        }        
    }
    
    /**
     * Обновить данные о пользователе
     * @param user - новые данные о пользователе
     */
    public void updateUserProfile(UserEntity user) {
        
        if (userRepository.existsById(user.getId())) {
            saveUser(user);
        } else {
            throw new UserNFException("User with id: " + user.getId() + "Not Found");
        }
    }

    /**
     * Получить курсы, на которые подписан пользователь (включая те, в которых он 
     * отмечен как автор)
     * @param userId - id пользователя, курсы которого мы хотим получить
     * @return - курсы, на которые подписан пользователь
     */
    public List<CourseEntity> getUserCourses(long userId) {

        ArrayList<CourseEntity> courses = new ArrayList<>();
        UserEntity user = getUser(userId);
       
        List<UserCourseRoleEntity> userCourseRoles = user.getUserCourseRoles();

        for (UserCourseRoleEntity ucr : userCourseRoles) {
            courses.add(ucr.getCourse());
        }
        
        return courses;
    }

    /**
     * Подписать пользователя на курсы
     * @param userId - id пользователя, которого надо подписать на курсы
     * @param courses - курсы, которые закрепим за пользователем
     */
    public void setUserCourses(long userId, List<CourseEntity> courses) {

    }


    /**
     * Получить все группы, в которых числится пользователь
     * @param userId - id пользователя, группы которого мы хотим получить
     * @return - список групп, в которых числится пользователь
     */
    public List<GroupEntity> getUserGroups(long userId) {

        UserEntity user = getUser(userId);
        
        return user.getGroups();
    }


    /**
     * Получить предпочитаемый пользователем язык
     * @param userId - id пользователя, предпочитаемый язык которого хотим узнать
     * @return - предпочитаемый пользователем язык
     */
    public Language getUserLanguage(long userId) {

        return null;
    }

    /**
     * Получить список всех глобальных ролей
     * @return
     */
    public ArrayList<Role> getGeneralRoles() {

        return new ArrayList<>();
    }



    public CourseRole getCourseRole(long userId, long courseId) {

        UserEntity user = getUser(userId);
        
        CourseRole cr = null;
        List<UserCourseRoleEntity> userCourseRoles = user.getUserCourseRoles();
        for (int i = 0; i < userCourseRoles.size(); i++) {
            
            if (userCourseRoles.get(i).getCourse().getId() == courseId) {

                cr = userCourseRoles.get(i).getCourseRole();
            }
        }
        
        return cr;
    }

    public void setCourseRole(long userId, long courseId, CourseRole role) {

    }

    public Role getRole(long userId) {

        return Role.STUDENT;
    }
}
