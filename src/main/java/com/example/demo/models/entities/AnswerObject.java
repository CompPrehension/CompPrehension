package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "AnswerObject")
public class AnswerObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hyperText")
    private String hyperText;

    @Column(name = "domainInfo")
    private String domainInfo;

    @Column(name = "isRightCol")
    private boolean isRightCol;

    @Column(name = "concept")
    private String concept;


    @OneToMany(mappedBy = "leftAnswerObject", fetch = FetchType.LAZY)
    private List<Response> responsesLeft;

    @OneToMany(mappedBy = "rightAnswerObject", fetch = FetchType.LAZY)
    private List<Response> responsesRight;


    @ManyToOne
    @JoinColumn(name = "answerObject_id")
    private Question question;
    
    
}
