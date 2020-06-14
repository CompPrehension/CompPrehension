package com.example.demo.Service;

import com.example.demo.models.Dao.ResponseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResponseService {
    private ResponseDao responseDao;

    @Autowired
    public ResponseService(ResponseDao responseDao) {
        this.responseDao = responseDao;
    }
}
