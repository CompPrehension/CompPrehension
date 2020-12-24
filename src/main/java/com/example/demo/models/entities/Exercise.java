package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.Complexity;
import com.example.demo.models.entities.EnumData.ExerciseType;
import com.example.demo.models.entities.EnumData.Language;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Exercise")
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //TODO: name и shortname
    @Column(name = "name")
    private String name;
    
    @Column(name = "maxRetries")
    private Integer maxRetries;

    @Column(name = "useGuidingQuestions")
    private Boolean useGuidingQuestions;

    @Column(name = "timeLimit")
    private Integer timeLimit;

    @Column(name = "hidden")
    private Boolean hidden;

    @Column(name = "tags")
    private String tags;


    @Column(name = "exerciseType")
    @Enumerated(EnumType.ORDINAL)
    private ExerciseType exerciseType;

    @Enumerated(EnumType.ORDINAL)
    private Complexity complexity;

    @Column(name = "language_id")
    @Enumerated(EnumType.ORDINAL)
    private Language language;


    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "backend_id", nullable = false)
    private Backend backend;

    @ManyToOne
    @JoinColumn(name = "domain_id", nullable = false)
    private DomainEntity domain;


    @OneToMany(mappedBy = "exercise")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<AdditionalField> additionalFields;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseDisplayingFeedbackType> exerciseDisplayingFeedbackTypes;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseQuestionType> exerciseQuestionTypes;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<UserActionExercise> userActionExercises;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseLaws> exerciseLaws;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseConcept> exerciseConcepts;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseAttempt> exerciseAttempts;


    @ManyToMany(mappedBy = "exercises")
    private List<User> users;
}
