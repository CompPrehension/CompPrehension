package com.example.demo.models.businesslogic;
import com.example.demo.models.entities.ExerciseAttempt;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.Response;

import java.util.HashMap;
import java.util.Map;

public class Core {
    
    private Map<Long, Domain> domainMap = new HashMap<Long, Domain>() {{
        put((long)1, new Programming());        
    }};   
    
    public Domain getDomain(long domainId) {
        
        return domainMap.get(domainId);
    }
    
    public void saveExercise(Exercise ex) {
        
        //Проверяем, есть ли у упражнения id
        //Записываем упражнение в базу
        //Если у упражнения был id
            //Создаем в базе действие: упражнение было отредактировано
        //Иначе
            //Создаем в базе действие: упражнение было создано
    }
    
    public ExerciseAttempt startExerciseAttempt(long exerciseId, long userId, FrontEndInfo frontEndInfo) {
        
        //Создаем в базе сущность ExerciseAttempt
        
        return null;
    }
    
    public void recieveResponse(long att_id, Response resp) {
        
    }
    
}
