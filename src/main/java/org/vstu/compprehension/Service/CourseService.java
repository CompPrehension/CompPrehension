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
