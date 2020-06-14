package com.example.demo.Service;

import com.example.demo.models.Dao.CourseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseService {
    private CourseDao courseDao;

    @Autowired
    public CourseService(CourseDao courseDao) {
        this.courseDao = courseDao;
    }
}
