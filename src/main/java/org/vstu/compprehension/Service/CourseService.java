package org.vstu.compprehension.Service;

import org.vstu.compprehension.Exceptions.NotFoundEx.CourseNFException;
import org.vstu.compprehension.Exceptions.NotFoundEx.UserNFException;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.entities.CourseEntity;
import org.vstu.compprehension.models.entities.EnumData.CourseRole;
import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.UserCourseRoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CourseService {
    
    private CourseRepository courseRepository;

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserCourseRoleService userCourseRoleService;
    
    @Autowired
    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }
    
    public Iterable<CourseEntity> getCoursesByUserId(Long userId) {
        
        return courseRepository.findAll();
    }

    /**
     * Добавить новый курс
     * @param course - данные о новом курсе
     * @param authorId - id создателя курса
     */
    public void addCourse(CourseEntity course , long authorId) {
        
        UserEntity user = userService.getUser(authorId);
        UserCourseRoleEntity userCourseRole = new UserCourseRoleEntity();
        userCourseRole.setUser(user);
        userCourseRole.setCourseRole(CourseRole.AUTHOR);
        userCourseRole.setCourse(course);
        course.getUserCourseRoles().add(userCourseRole);
        user.getUserCourseRoles().add(userCourseRole);
        courseRepository.save(course);
        userService.updateUserProfile(user);
    }

    public List<UserEntity> getStudents(long courseId) {
        //Взять из базы пользователей с ролью - студент
        ArrayList<UserEntity> students = new ArrayList<>();
        CourseEntity course = getCourse(courseId);
        List<UserCourseRoleEntity> userCourseRoles = course.getUserCourseRoles();

        for (UserCourseRoleEntity ucr : userCourseRoles) {
            
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
    public void addCourse(String name, String description, long author_id) {

        CourseEntity newCourse = new CourseEntity();
        newCourse.setName(name);
        newCourse.setDescription(description);
        UserEntity user = userService.getUser(author_id);

        UserCourseRoleEntity userCourseRole = new UserCourseRoleEntity();
        userCourseRole.setCourse(newCourse);
        userCourseRole.setCourseRole(CourseRole.AUTHOR);
        userCourseRole.setUser(user);
        
        user.getUserCourseRoles().add(userCourseRole);
        newCourse.getUserCourseRoles().add(userCourseRole);

        userCourseRoleService.saveUserCourseRole(userCourseRole);
        userService.updateUserProfile(user);
        courseRepository.save(newCourse);
        
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
    public void updateCourse(CourseEntity course) {
        
        if (!courseRepository.existsById(course.getId())) {
            throw new CourseNFException("Course with id: " + course.getId()
                    + "Not Found");
        }
        
        courseRepository.save(course);
    }


    /**
     * Получить информацию о курсе
     * @param courseId - id курса, о котором хотим получить информацию
     * @return - информация о курсе
     */
    public CourseEntity getCourse(long courseId) {
        try {
            return courseRepository.findById(courseId).orElseThrow(()->
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

    public List<ExerciseEntity> getExercises(long courseId){

        CourseEntity c = getCourse(courseId);
        
        if (c == null) { return new ArrayList<>(); }
        
        return c.getExercises();
    }
}
