package com.example.demo.models.entities;

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
