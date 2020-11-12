package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionConceptOrder {

    private String startConcept;

    private String notInOrderConcept;

    private String followVerb;

    private String notInOrderVerb;

    private Question question;

    private Backend backend;
}
