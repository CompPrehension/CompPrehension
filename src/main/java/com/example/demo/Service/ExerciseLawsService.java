package com.example.demo.Service;

import com.example.demo.models.Dao.ExerciseLawsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseLawsService {
    private ExerciseLawsDao exerciseLawsDao;

    @Autowired
    public ExerciseLawsService(ExerciseLawsDao exerciseLawsDao) {
        this.exerciseLawsDao = exerciseLawsDao;
    }
}
