package com.example.demo.Service;

import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.CourseRole;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.Role;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.Dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private UserDao userDao;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserCourseRoleService userCourseRoleService;
    
    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User getUserByEmail(String email){
        try{
            return userDao.findUserByEmail(email).orElseThrow(()->
                    new UserNFException("User with email: " + email + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }
    
    public User getUserByLogin(String login) {
        try{
            return userDao.findUserByLogin(login).orElseThrow(()->
                    new UserNFException("User with login: " + login + "Not Found"));
        } catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }

    public long checkUserData(String login, String password) {
        
        User user;
        try { user = getUser(login); } 
        catch (UserNFException e) { return -1; }
        //Если пользователь не найден или у найденного пользователя не тот пароль
        if (user == null || !user.getPassword().equals(password)) { return -1; }

        return user.getId();
    }

    public User getUser(long userId) { 
        try {  
            return userDao.findUserById(userId).orElseThrow(()->
                    new UserNFException("User with id: " + userId + "Not Found"));
        } catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }

    public User getUser(String login) { 
        try {
            return userDao.findUserByLogin(login).orElseThrow(()->
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

        User user = getUser(userId);
        Course course = courseService.getCourse(courseId);
        UserCourseRole userCourseRole = new UserCourseRole();
        userCourseRole.setUser(user);
        userCourseRole.setCourseRole(role);
        userCourseRole.setCourse(course);

        course.getUserCourseRoles().add(userCourseRole);
        user.getUserCourseRoles().add(userCourseRole);
        
        courseService.updateCourse(course);
        userDao.save(user);
        userCourseRoleService.saveUserCourseRole(userCourseRole);
    }


    /**
     * @param userId - id пользователя, действия которого хотим получить
     * @return - список действий пользователя
     */
    public List<UserAction> getUserActions(long userId) {

        return getUser(userId).getUserActions();
    }

    public List<Action> getUserActionsToFront(long userId) {
        
        List<Action> actions = new ArrayList<>();
        User user = getUser(userId);
        for (UserAction ua : getUserActions(userId)) {
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

    public void saveUser(User user) {
        try {
            if (getUser(user.getLogin()) != null) {
                throw new DataIntegrityViolationException("Пользователь с таким логином " +
                        "уже существует");
            }
        } catch (UserNFException e) {
            userDao.save(user);
        }        
    }
    
    /**
     * Обновить данные о пользователе
     * @param user - новые данные о пользователе
     */
    public void updateUserProfile(User user) { 
        
        if (userDao.existsById(user.getId())) {
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
    public List<Course> getUserCourses(long userId) {

        ArrayList<Course> courses = new ArrayList<>();
        User user = getUser(userId);
       
        List<UserCourseRole> userCourseRoles = user.getUserCourseRoles();

        for (UserCourseRole ucr : userCourseRoles) {
            courses.add(ucr.getCourse());
        }
        
        return courses;
    }

    /**
     * Подписать пользователя на курсы
     * @param userId - id пользователя, которого надо подписать на курсы
     * @param courses - курсы, которые закрепим за пользователем
     */
    public void setUserCourses(long userId, List<Course> courses) {

    }


    /**
     * Получить все группы, в которых числится пользователь
     * @param userId - id пользователя, группы которого мы хотим получить
     * @return - список групп, в которых числится пользователь
     */
    public List<Group> getUserGroups(long userId) {

        User user = getUser(userId);
        
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

        User user = getUser(userId);
        
        CourseRole cr = null;
        List<UserCourseRole> userCourseRoles = user.getUserCourseRoles();
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
