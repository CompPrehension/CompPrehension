package org.vstu.compprehension.Service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.EducationResourceRepository;

import java.util.Optional;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final EducationResourceRepository educationResourceRepository;

    @Autowired
    public CourseService(CourseRepository courseRepository, EducationResourceRepository educationResourceRepository) {
        this.courseRepository = courseRepository;
        this.educationResourceRepository = educationResourceRepository;
    }

    public long getInitialCourseId() {
        try {
            var initialCourse = courseRepository.findByName("global").orElse(null);
            return initialCourse.getId();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Database is not initialized and does not contain global course");
        }
    }

    public CourseEntity getOrCreateCourse(String name, Long educationResourceId) {
        Optional<CourseEntity> foundCourse = courseRepository.findByName(name);
        if (foundCourse.isPresent()) {
            return foundCourse.get();
        }

        var educationResource = educationResourceRepository.findById(educationResourceId);
        if (educationResource.isEmpty()) {
            throw new EntityNotFoundException("Education resource not found");
        }

        CourseEntity course = new CourseEntity();
        course.setName(name);
        course.setEducationResources(educationResource.get());
        return courseRepository.save(course);
    }
}
