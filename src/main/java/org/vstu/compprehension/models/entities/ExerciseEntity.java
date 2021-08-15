package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.entities.EnumData.Complexity;
import org.vstu.compprehension.models.entities.EnumData.ExerciseType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    @Column(name = "timeLimit")
    private Integer timeLimit;

    @Column(name = "hidden")
    private Boolean hidden;

    @Column(name = "tags")
    private String tags;

    @Type(type = "json")
    @Column(name = "options_json", columnDefinition = "json", nullable = false)
    private ExerciseOptionsEntity options;

    public List<Tag> getTags() {
        return Arrays.stream(tags.split(","))
                .map(i -> new Tag(i))
                .collect(Collectors.toList());
    }


    @Column(name = "exerciseType")
    @Enumerated(EnumType.ORDINAL)
    private ExerciseType exerciseType;

    @Enumerated(EnumType.ORDINAL)
    private Complexity complexity;

    @Column(name = "language_id")
    @Enumerated(EnumType.ORDINAL)
    private Language language;

    @ManyToOne
    @JoinColumn(name = "backend_id", nullable = false)
    private BackendEntity backend;

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
