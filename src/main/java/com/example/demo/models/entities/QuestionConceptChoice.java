package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "QuestionConceptChoice")
public class QuestionConceptChoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "selectedVerb")
    private String selectedVerb;

    @Column(name = "notSelectedVerb")
    private String notSelectedVerb;

    @Column(name = "selectedConcept")
    private String selectedConcept;

    @Column(name = "notSelectedConcept")
    private String notSelectedConcept;


    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;


    @ManyToOne
    @JoinColumn(name = "questionConceptChoice_id", nullable = false)
    private Backend backend;


}
