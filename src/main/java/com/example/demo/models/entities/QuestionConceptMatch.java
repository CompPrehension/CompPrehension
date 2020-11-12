package com.example.demo.models.entities;

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

    private Question question;

    private Backend backend;
}
