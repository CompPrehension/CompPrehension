package com.example.demo.Service;

import com.example.demo.models.Dao.MistakeDao;
import com.example.demo.models.entities.Mistake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MistakeService {
    private MistakeDao mistakeDao;

    @Autowired
    public MistakeService(MistakeDao mistakeDao) {
        this.mistakeDao = mistakeDao;
    }
    
    public void saveMistake(Mistake mistake) {
        
        mistakeDao.save(mistake);
    }
}
