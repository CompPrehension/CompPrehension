package com.example.demo.models.businesslogic.questionconcept;

import com.example.demo.models.entities.BackendEntity;
import com.example.demo.models.entities.QuestionEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionConceptChoice {
    private String selectedVerb;

    private String notSelectedVerb;

    private String selectedConcept;

    private String notSelectedConcept;

    private QuestionEntity question;

    private BackendEntity backend;
}
