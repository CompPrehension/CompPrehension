package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "BackendFacts")
public class BackendFact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object")
    private String object;
    @Column(name = "object_type")
    private String objectType;

    @Column(name = "subject")
    private String subject;
    @Column(name = "subject_type")
    private String subjectType;

    @Column(name = "verb")
    private String verb;

    @ManyToOne
    @JoinColumn(name = "BackendFact_id")
    private Question question;

    public BackendFact(String subjectType, String subject, String verb, String objectType, String object) {
        this.object = object;
        this.objectType = objectType;
        this.subject = subject;
        this.subjectType = subjectType;
        this.verb = verb;
    }

    public BackendFact(String subject, String verb, String object) {
        this.object = object;
        this.objectType = "";
        this.subject = subject;
        this.subjectType = "";
        this.verb = verb;
    }
}
