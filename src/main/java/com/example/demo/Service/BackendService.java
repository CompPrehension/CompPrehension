package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.BackendNFException;
import com.example.demo.models.repository.BackendRepository;
import com.example.demo.models.entities.Backend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class BackendService {
    private BackendRepository backendRepository;

    @Autowired
    public BackendService(BackendRepository backendRepository) {
        this.backendRepository = backendRepository;
    }
    
    public Backend getDefaultBackend() {

        Iterator<Backend> iterator = backendRepository.findAll().iterator();
        if (iterator.hasNext()) { return iterator.next(); }
        else { throw new BackendNFException("В базе нет backend-а по умолчанию"); }
        
    }
}
