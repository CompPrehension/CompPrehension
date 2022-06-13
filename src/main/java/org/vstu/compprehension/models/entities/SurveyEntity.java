package org.vstu.compprehension.models.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter @Setter
@Table(name = "surveys")
public class SurveyEntity {
    @Id
    @Column(name = "id", nullable = false, length = 255)
    private @NotNull String surveyId;

    @Column(name = "name", nullable = false, length = 255)
    private @NotNull String name;

    @Type(type = "json")
    @Column(name = "options_json", columnDefinition = "json", nullable = false)
    private SurveyOptionsEntity options;

    @OneToMany(mappedBy = "survey", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<SurveyQuestionEntity> questions;
}
