package org.vstu.compprehension.models.entities.exercise;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.EnumData.ExerciseType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.ExerciseQuestionTypeEntity;
import org.vstu.compprehension.models.entities.UserEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Exercise")
public class ExerciseEntity implements Cloneable {
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

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

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "model_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID modelId;

    public List<String> getTags() {
        return Arrays.stream(tags.split("\\s*,\\s*"))
                .filter(t -> !StringHelper.isNullOrWhitespace(t))
                .collect(Collectors.toList());
    }


    @Column(name = "exerciseType")
    @Enumerated(EnumType.ORDINAL)
    private ExerciseType exerciseType;

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

    @Override
    public ExerciseEntity clone() {
        try {
            ExerciseEntity copy = (ExerciseEntity) super.clone();
            copy.setName(this.name);
            copy.setDomain(this.domain);
            copy.setBackendId(this.backendId);
            copy.setStrategyId(this.strategyId);
            copy.setLanguage(this.language);
            copy.setMaxRetries(this.maxRetries);
            copy.setUseGuidingQuestions(this.useGuidingQuestions);
            copy.setHidden(this.hidden);
            copy.setTags(this.tags);
            copy.setExerciseType(this.exerciseType);
            copy.setModelId(this.modelId);
            // isPublic defaults to false; caller sets it explicitly if the clone should be public
            copy.setOptions(this.options);
            copy.setStages(this.stages == null ? new ArrayList<>() : new ArrayList<>(this.stages));
            if (this.exerciseQuestionTypes != null) {
                copy.setExerciseQuestionTypes(
                        this.exerciseQuestionTypes.stream()
                                .map(t -> new ExerciseQuestionTypeEntity(copy, t.getQuestionType()))
                                .collect(Collectors.toCollection(ArrayList::new))
                );
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
