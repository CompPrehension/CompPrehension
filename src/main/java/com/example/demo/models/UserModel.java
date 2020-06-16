package com.example.demo.models;

import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.Service.CourseService;
import com.example.demo.Service.UserService;
import com.example.demo.models.Dao.UserDao;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.CourseRole;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.Role;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserModel {
    
    //@Autowired
    //private UserService userService;
//
    //@Autowired
    //private CourseService courseService;
    //
    //public long checkUserData(String login, String password) {
    //    
    //    User user = userService.getUserByLogin(login); 
    //    
    //    //Если пользователь не найден или у найденного пользователя не тот пароль
    //    if (user == null || !user.getPassword().equals(password)) { return -1; }
    //    
    //    return user.getId();
    //}
    //
    //public User getUser(long userId) { return userService.getUserById(userId); }
//
    //public User getUser(String login) { return userService.getUserByLogin(login); }
//
//
    ///**
    // * Удалить пользователя с курса
    // * @param userId - id пользователя, которого будем удалять с курса
    // * @param courseId - id курса, с которого будем удалять пользователя
    // */
    //public void removeFromCourse(long userId, long courseId) {
//
    //}
//
//
    ///**
    // * Добавить пользователя на курс (по умолчанию добавляется пользователь
    // * с ролью - студент)
    // * @param userId - id пользователя, которого будем добавлять
    // * @param courseId - id курса, куда будем добавлять пользователя
    // */
    //public void addToCourse(long userId, long courseId) {
    //    
    //}
//
    ///**
    // * Добавить пользователя на курс
    // * @param userId - id пользователя, которого будем добавлять
    // * @param courseId - id курса, куда будем добавлять пользователя
    // * @param role - роль пользователя в рамках курса
    // */
    //public void addToCourse(long userId, long courseId, CourseRole role) {
    //    
    //}
    //
    //public ArrayList<User> getStudents(long courseId) {
    //    //Взять из базы студентов с ролью - студент
    //    return null;
    //}
//
//
    ///**
    // * @param userId - id пользователя, действия которого хотим получить
    // * @return - список действий пользователя
    // */
    //public ArrayList<Action> getUserActions(long userId) {
    //    
    //    return null;
    //}
//
//
    ///**
    // * Обновить данные о пользователе
    // * @param user - новые данные о пользователе
    // */
    //public void updateUserProfile(User user) {
    //    
    //    
    //}
//
    ///**
    // * Получить курсы, на которые подписан пользователь (включая те, в которых он 
    // * отмечен как автор)
    // * @param userId - id пользователя, курсы которого мы хотим получить
    // * @return - курсы, на которые подписан пользователь
    // */
    //public ArrayList<Course> getUserCourses(long userId) {
//
    //    //return courseService.;
    //}
//
    ///**
    // * Подписать пользователя на курсы
    // * @param userId - id пользователя, которого надо подписать на курсы
    // * @param courses - курсы, которые закрепим за пользователем
    // */
    //public void setUserCourses(long userId, List<Course> courses) {
//
    //}
//
//
    ///**
    // * Получить все группы, в которых числится пользователь
    // * @param userId - id пользователя, группы которого мы хотим получить
    // * @return - список групп, в которых числится пользователь
    // */
    //public ArrayList<Group> getUserGroups(long userId) {
    //    
    //    return null;
    //}
//
//
    ///**
    // * Получить предпочитаемый пользователем язык
    // * @param userId - id пользователя, предпочитаемый язык которого хотим узнать
    // * @return - предпочитаемый пользователем язык
    // */
    //public Language getUserLanguage(long userId) {
    //    
    //    return null;
    //}
//
    ///**
    // * Получить список всех глобальных ролей
    // * @return
    // */
    //public ArrayList<Role> getGeneralRoles() {
//
    //    return new ArrayList<>();
    //}
//
//
    //
    //public CourseRole getCourseRole(long userId, long courseId) {
//
    //    return CourseRole.STUDENT;
    //}
//
    //public void setCourseRole(long userId, long courseId, CourseRole role) {
//
    //}
//
    //public Role getRole(long userId) {
//
    //    return Role.STUDENT;
    //}
}
