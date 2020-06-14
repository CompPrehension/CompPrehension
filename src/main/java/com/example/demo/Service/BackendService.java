package com.example.demo.Service;

import com.example.demo.models.Dao.BackendDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BackendService {
    private BackendDao backendDao;

    @Autowired
    public BackendService(BackendDao backendDao) {
        this.backendDao = backendDao;
    }
}
