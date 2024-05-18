package org.vstu.compprehension.models.entities.exercise;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.EnumData.ExerciseType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.ExerciseQuestionTypeEntity;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.course.CourseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Exercise")
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


    @Column(name = "hidden")
    private Boolean hidden;

    @Column(name = "tags", nullable = false)
    private String tags;

    @Type(JsonType.class)
    @Column(name = "options_json", columnDefinition = "json", nullable = false)
    private ExerciseOptionsEntity options;

    @Type(JsonType.class)
    @Column(name = "stages_json", columnDefinition = "json", nullable = false)
    private List<ExerciseStageEntity> stages;

    @Column(name = "backend_id", nullable = false, length = 100)
    private @NotNull String backendId;

    @Column(name = "strategy_id", nullable = false, length = 100)
    private @NotNull String strategyId;

    public List<String> getTags() {
        return Arrays.stream(tags.split("\\s*,\\s*"))
                .filter(t -> !StringHelper.isNullOrWhitespace(t))
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

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseQuestionTypeEntity> exerciseQuestionTypes;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<ExerciseAttemptEntity> exerciseAttempts;

    @ManyToMany(mappedBy = "exercises", fetch = FetchType.LAZY)
    private List<UserEntity> users;

    // This field is actually not null. It gets this constraint after initialization in migration script
    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseEntity course;
}
