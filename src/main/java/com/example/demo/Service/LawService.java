package com.example.demo.Service;

import com.example.demo.models.Dao.LawDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LawService {
    private LawDao lawDao;

    @Autowired
    public LawService(LawDao lawDao) {
        this.lawDao = lawDao;
    }
}
