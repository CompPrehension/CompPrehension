package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.CourseNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.Dao.CourseDao;
import com.example.demo.models.entities.Course;
import com.example.demo.models.entities.EnumData.CourseRole;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.User;
import com.example.demo.models.entities.UserCourseRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Service
public class CourseService {
    
    private CourseDao courseDao;

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserCourseRoleService userCourseRoleService;
    
    @Autowired
    public CourseService(CourseDao courseDao) {
        this.courseDao = courseDao;
    }
    
    public Iterable<Course> getCoursesByUserId(Long userId) {
        
        return courseDao.findAll();
    }

    /**
     * Добавить новый курс
     * @param course - данные о новом курсе
     * @param author_id - id создателя курса
     */
    public void addCourse(Course course , long author_id) {

    }

    public List<User> getStudents(long courseId) {
        //Взять из базы пользователей с ролью - студент
        ArrayList<User> students = new ArrayList<>();
        Course course = getCourse(courseId);
        List<UserCourseRole> userCourseRoles = course.getUserCourseRoles();

        for (UserCourseRole ucr : userCourseRoles) {
            
            if (ucr.getCourseRole() == CourseRole.STUDENT) {
                
                students.add(ucr.getUser());
            }
        }
        
        return students;
    }

    /**
     * Добавить новый курс
     * @param name - название курса
     * @param description - описание курса
     * @param author_id - id автора курса
     */
    public void addCourse(String name, String description , long author_id) {

        Course newCourse = new Course();
        newCourse.setName(name);
        newCourse.setDescription(description);
        User user = userService.getUser(author_id);

        UserCourseRole userCourseRole = new UserCourseRole();
        userCourseRole.setCourse(newCourse);
        userCourseRole.setCourseRole(CourseRole.AUTHOR);
        userCourseRole.setUser(user);
        
        user.getUserCourseRoles().add(userCourseRole);
        newCourse.getUserCourseRoles().add(userCourseRole);

        userCourseRoleService.saveUserCourseRole(userCourseRole);
        userService.updateUserProfile(user);
        courseDao.save(newCourse);
        
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
        
        if (courseDao.existsById(course.getId())) {
            throw new CourseNFException("Course with id: " + course.getId() 
                    + "Not Found");
        }
        
        courseDao.save(course);
    }


    /**
     * Получить информацию о курсе
     * @param courseId - id курса, о котором хотим получить информацию
     * @return - информация о курсе
     */
    public Course getCourse(long courseId) {
        try {
            return courseDao.findCourseById(courseId).orElseThrow(()->
                    new CourseNFException("Course with id: " + courseId + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-course to Model-course", e);
        }    
    }

    /**
     * Получить список всех возможных ролей в рамках курса 
     * @return - список всех возможных ролей в рамках курса
     */
    public List<CourseRole> getCourseRoles() {

        return Arrays.asList(CourseRole.values());
    }

    public List<Exercise> getExercises(long courseId){

        Course c = getCourse(courseId);
        
        if (c == null) { return new ArrayList<>(); }
        
        return c.getExercises();
    }
}
