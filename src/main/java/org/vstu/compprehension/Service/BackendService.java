package org.vstu.compprehension.Service;

import org.vstu.compprehension.Exceptions.NotFoundEx.BackendNFException;
import org.vstu.compprehension.models.repository.BackendRepository;
import org.vstu.compprehension.models.entities.BackendEntity;
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
    
    public BackendEntity getDefaultBackend() {

        Iterator<BackendEntity> iterator = backendRepository.findAll().iterator();
        if (iterator.hasNext()) { return iterator.next(); }
        else { throw new BackendNFException("В базе нет backend-а по умолчанию"); }
        
    }
}
