package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.entities.EnumData.Complexity;
import org.vstu.compprehension.models.entities.EnumData.ExerciseType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Exercise")
@TypeDef(name = "json", typeClass = JsonStringType.class)
public class ExerciseEntity {
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

    @Column(name = "numberOfQuestions", nullable = false)
    private Integer numberOfQuestions;

    /**
     * normalized question length, steps (нормированная длина вопроса в шагах)
     * range: [0..10]
     */
    @Column(name = "timeLimit")
    private Integer timeLimit;

    @Column(name = "hidden")
    private Boolean hidden;

    @Column(name = "tags")
    private String tags;

    @Type(type = "json")
    @Column(name = "options_json", columnDefinition = "json", nullable = false)
    private ExerciseOptionsEntity options;

    @Column(name = "backend_id", nullable = false, length = 100)
    private @NotNull String backendId;

    @Column(name = "strategy_id", nullable = false, length = 100)
    private @NotNull String strategyId;

    public List<Tag> getTags() {
        return Arrays.stream(tags.split(","))
                .map(Tag::new)
                .collect(Collectors.toList());
    }


    @Column(name = "exerciseType")
    @Enumerated(EnumType.ORDINAL)
    private ExerciseType exerciseType;

    /** Desired "integral" complexity for questions: [0..1] */
    @Column(name = "complexity", nullable = false)
    private Float complexity;

    @Column(name = "language_id")
    @Enumerated(EnumType.ORDINAL)
    private Language language;

    @ManyToOne
    @JoinColumn(name = "domain_id", nullable = false)
    private DomainEntity domain;

    @OneToMany(mappedBy = "exercise")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<AdditionalFieldEntity> additionalFields;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseQuestionTypeEntity> exerciseQuestionTypes;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseLawsEntity> exerciseLaws;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseConceptEntity> exerciseConcepts;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseAttemptEntity> exerciseAttempts;

    @ManyToMany(mappedBy = "exercises", fetch = FetchType.LAZY)
    private List<UserEntity> users;
}
