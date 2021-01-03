package com.example.demo.models.businesslogic.questionconcept;

import com.example.demo.models.entities.BackendEntity;
import com.example.demo.models.entities.QuestionEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionConceptMatch {
    private String matchVerb;

    private String noMatchLeftConcept;

    private String noMatchLeftVerb;

    private String noMatchRightConcept;

    private String noMatchRightVerb;

    private QuestionEntity question;

    private BackendEntity backend;
}
