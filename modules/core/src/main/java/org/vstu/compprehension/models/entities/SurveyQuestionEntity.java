package org.vstu.compprehension.models.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Getter @Setter
@Table(name = "survey_questions")
public class SurveyQuestionEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false, length = 100)
    private String type;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Type(type = "json")
    @Column(name = "policy", columnDefinition = "json", nullable = false)
    private Object policy;

    @Type(type = "json")
    @Column(name = "options_json", columnDefinition = "json", nullable = false)
    private Object options;

    @ManyToOne
    @JoinColumn(name = "survey_id")
    @ToString.Exclude
    private SurveyEntity survey;
}
