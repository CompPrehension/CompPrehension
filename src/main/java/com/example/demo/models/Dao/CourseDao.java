package com.example.demo.models.Dao;


import com.example.demo.models.entities.Course;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CourseDao extends CrudRepository<Course, Long> {
    Optional<Course> findCourseById(Long id);
}
