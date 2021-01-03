package com.example.demo.models.businesslogic.questionconcept;

import com.example.demo.models.entities.BackendEntity;
import com.example.demo.models.entities.QuestionEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionConceptOrder {

    private String startConcept;

    private String notInOrderConcept;

    private String followVerb;

    private String notInOrderVerb;

    private QuestionEntity question;

    private BackendEntity backend;
}
