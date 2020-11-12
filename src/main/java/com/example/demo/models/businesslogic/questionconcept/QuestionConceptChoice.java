package com.example.demo.models.businesslogic.questionconcept;

import com.example.demo.models.entities.Backend;
import com.example.demo.models.entities.Question;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionConceptChoice {
    private String selectedVerb;

    private String notSelectedVerb;

    private String selectedConcept;

    private String notSelectedConcept;

    private Question question;

    private Backend backend;
}
