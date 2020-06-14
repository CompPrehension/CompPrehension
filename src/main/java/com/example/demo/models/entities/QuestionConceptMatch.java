package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "QuestionConceptMatch")
public class QuestionConceptMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matchVerb")
    private String matchVerb;

    @Column(name = "noMatchLeftConcept")
    private String noMatchLeftConcept;

    @Column(name = "noMatchLeftVerb")
    private String noMatchLeftVerb;

    @Column(name = "noMatchRightConcept")
    private String noMatchRightConcept;

    @Column(name = "noMatchRightVerb")
    private String noMatchRightVerb;


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;


    @ManyToOne
    @JoinColumn(name = "questionConceptMatch_id", nullable = false)
    private Backend backend;
}
