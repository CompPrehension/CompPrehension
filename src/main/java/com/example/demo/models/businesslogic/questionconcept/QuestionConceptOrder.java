package com.example.demo.models.businesslogic.questionconcept;

import com.example.demo.models.entities.Backend;
import com.example.demo.models.entities.Question;
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
