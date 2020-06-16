package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.BackendNFException;
import com.example.demo.models.Dao.BackendDao;
import com.example.demo.models.entities.Backend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class BackendService {
    private BackendDao backendDao;

    @Autowired
    public BackendService(BackendDao backendDao) {
        this.backendDao = backendDao;
    }
    
    public Backend getDefaultBackend() {

        Iterator<Backend> iterator = backendDao.findAll().iterator();
        if (iterator.hasNext()) { return iterator.next(); }
        else { throw new BackendNFException("В базе нет backend-а по умолчанию"); }
        
    }
}
