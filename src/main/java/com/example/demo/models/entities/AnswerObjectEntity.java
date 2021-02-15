package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "AnswerObject")
public class AnswerObjectEntity {
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
    private List<ResponseEntity> responsesLeft;

    @OneToMany(mappedBy = "rightAnswerObject", fetch = FetchType.LAZY)
    private List<ResponseEntity> responsesRight;


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "answerObject_id")
    private QuestionEntity question;
    
    
}
