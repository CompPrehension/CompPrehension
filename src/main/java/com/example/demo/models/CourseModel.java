package com.example.demo.models;

import com.example.demo.models.entities.Course;
import com.example.demo.models.entities.EnumData.CourseRole;

import java.util.ArrayList;

public class CourseModel {

    /** 
     * Добавить новый курс
     * @param course - данные о новом курсе
     * @param author_id - id создателя курса
     */
    public void addCourse(Course course , long author_id) {

    }


    /** 
     * Добавить новый курс
     * @param name - название курса
     * @param description - описание курса
     * @param author_id - id автора курса
     */
    public void addCourse(String name, String description , long author_id) {

    }
    
    public void addCourse(long userId, long courseId , long author_id) {

    }

    /**
     * Удалить курс
     * @param courseId - id курса который хотим удалить 
     */
    public void removeCourse(long courseId) {
        
    }

    /**
     * Обновить информацию о курсе
     * @param course  - новая информация о курсе
     */
    public void updateCourse(Course course) {

    }


    /**
     * Получить информацию о курсе
     * @param courseId - id курса, о котором хотим получить информацию
     * @return - информация о курсе
     */
    public Course getCourse(long courseId) {
        
        return null;
    }

    /**
     * Получить список всех возможных ролей в рамках курса
     * @return - список всех возможных ролей в рамках курса
     */
    public ArrayList<CourseRole> getCourseRoles() {

        return new ArrayList<>();
    }


}
