package org.vstu.compprehension.models.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter @Setter
@Entity
@Table(name = "ctfl_questions")
public class CtrlFlowQuestionEntity implements QuestionMetadataBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    // @Lob
    @Column(name = "name")
    private String name;

    @Column(name = "template_id", nullable = false)
    private Integer templateId;

    // @Lob
    @Column(name = "q_data_graph")
    private String qDataGraph;

    @Column(name = "tag_bits")
    private Integer tagBits;

    @Column(name = "concept_bits")
    private Integer conceptBits;

    @Column(name = "law_bits")
    private Integer lawBits;

    @Column(name = "violation_bits")
    private Integer violationBits;

    @Column(name = "solution_structural_complexity")
    private Double solutionStructuralComplexity;

    @Column(name = "integral_complexity")
    private Double integralComplexity;

    @Column(name = "solution_steps")
    private Integer solutionSteps;

    @Column(name = "distinct_errors_count")
    private Integer distinctErrorsCount;

    @Column(name = "_stage")
    private Integer stage;

    @Column(name = "_version")
    private Integer version;

}