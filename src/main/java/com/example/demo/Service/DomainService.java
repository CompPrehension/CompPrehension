package com.example.demo.Service;

import com.example.demo.models.Dao.DomainDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DomainService {
    private DomainDao domainDao;

    @Autowired
    public DomainService(DomainDao domainDao) {
        this.domainDao = domainDao;
    }
}
