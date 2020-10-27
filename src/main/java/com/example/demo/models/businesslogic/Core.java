package com.example.demo.models.businesslogic;
import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Service.DomainService;
import com.example.demo.models.businesslogic.backend.Backend;
import com.example.demo.models.businesslogic.backend.PelletBackend;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class Core {
    
    @Autowired
    private DomainService domainService;
    
    //TODO: Не работает, уточнить
    private Map<Long, Domain> domainMap/* = new HashMap<Long, Domain>() {
        {
            put((long)0, new TestDomain(domainService.getDomain((long)0)));
        }
    }*/;
    
    private PelletBackend pelletBackend = new PelletBackend();
    
    public Backend getDefaultBackend() {
        
        return pelletBackend;
    }
    
    public Domain getDomain(long domainId) {
        
        if (!domainMap.containsKey(domainId)) {
            
            throw new DomainNFException("Domain with id: " + domainId + "Not Found");
        }
        
        return domainMap.get(domainId);
    }
    
    /*public void saveExercise(Exercise ex) {
        
        //Проверяем, есть ли у упражнения id
        //Записываем упражнение в базу
        //Если у упражнения был id
            //Создаем в базе действие: упражнение было отредактировано
        //Иначе
            //Создаем в базе действие: упражнение было создано
    }*/
    
    /*public ExerciseAttempt startExerciseAttempt(long exerciseId, long userId, FrontEndInfo frontEndInfo) {
        
        //Создаем в базе сущность ExerciseAttempt
        
        return null;
    }*/
    
    /*public void receiveResponse(long att_id, Response resp) {
        
    }*/    
    
    
}
