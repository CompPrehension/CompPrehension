package com.example.demo.models.businesslogic.frontend;

import com.example.demo.models.entities.AnswerObjectEntity;
import lombok.Data;

import java.util.Map;

@Data
public class QuestionMistakes {

    /**
     * Объяснение к ошибкам, которые были допущены при ответе (пустая строка, 
     * если ошибок не было) 
     */
    private String explanation;

    /**
     * Объект ответа (в основном это вариант ответа, но для соответствия еще
     * и элемент левой колонки), и является ли он правильным.
     */
    private Map<AnswerObjectEntity, Boolean> mistakes;
    
    
}
