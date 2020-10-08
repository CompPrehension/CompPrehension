package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.QuestionStatus;
import com.example.demo.models.entities.EnumData.QuestionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Question")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.ORDINAL)
    private QuestionType questionType;

    @Enumerated(EnumType.ORDINAL)
    private QuestionStatus questionStatus;

    @Column(name = "questionText")
    private String questionText;

    @Column(name = "answersRequireContext")
    private Boolean areAnswersRequireContext;

    @OneToMany(mappedBy = "question")
    private List<QuestionConceptChoice> questionConceptChoices;//TODO: Проверить ОДИН К ОДНОМУ

    @OneToMany(mappedBy = "question")
    private List<QuestionConceptOrder> questionConceptOrders;//TODO: Проверить ОДИН К ОДНОМУ

    @OneToMany(mappedBy = "question")
    private List<QuestionConceptMatch> questionConceptMatches;//TODO: Проверить ОДИН К ОДНОМУ

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<QuestionAttempt> questionAttempts;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<AnswerObject> answerObjects;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "QuestionLaw",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "law_id"))
    private List<Law> laws;

}
