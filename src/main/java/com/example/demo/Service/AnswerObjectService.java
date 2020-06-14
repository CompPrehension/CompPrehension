package com.example.demo.Service;

import com.example.demo.models.Dao.AnswerObjectDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnswerObjectService {
    private AnswerObjectDao answerObjectDao;

    @Autowired
    public AnswerObjectService(AnswerObjectDao answerObjectDao) {
        this.answerObjectDao = answerObjectDao;
    }
}
