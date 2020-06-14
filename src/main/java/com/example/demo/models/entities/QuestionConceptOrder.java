package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "QuestionConceptOrder")
public class QuestionConceptOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startConcept")
    private String startConcept;

    @Column(name = "notInOrderConcept")
    private String notInOrderConcept;

    @Column(name = "followVerb")
    private String followVerb;

    @Column(name = "notInOrderVerb")
    private String notInOrderVerb;


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;


    @ManyToOne
    @JoinColumn(name = "questionConceptOrder_id", nullable = false)
    private Backend backend;
}
