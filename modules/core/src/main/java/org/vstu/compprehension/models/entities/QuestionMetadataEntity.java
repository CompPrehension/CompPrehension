package org.vstu.compprehension.models.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter @Setter
@Entity
@Table(name = "questions_meta")
public class QuestionMetadataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    // @Lob
    @Column(name = "name")
    private String name;

    @Column(name = "domain_shortname")
    private String domainShortname;

    @Column(name = "template_id", nullable = false)
    private Integer templateId;

    @Column(name = "q_data_graph")
    private String qDataGraph;

    @Column(name = "tag_bits")
    private Long tagBits;

    @Column(name = "concept_bits")
    private Long conceptBits;

    @Column(name = "law_bits")
    private Long lawBits;

    @Column(name = "violation_bits")
    private Long violationBits;

    @Column(name = "trace_concept_bits")
    private Long traceConceptBits;

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

    @Transient
    private Long conceptBitsInRequest = 0L;

    public double complexityAbsDiff(double complexity) {
        return Math.abs(getIntegralComplexity() - complexity);
    }
    public double getSolutionStepsAbsDiff(double steps) {
        return Math.abs(getSolutionSteps() - steps);
    }
    public Long traceConceptsSatisfiedFromRequest() {
        return (traceConceptBits != 0 ? traceConceptBits : conceptBits) & conceptBitsInRequest;  // traceConceptBits are not applicable to all domains...
    }
    public Long traceConceptsUnsatisfiedFromRequest() {
        return ~(traceConceptBits != 0 ? traceConceptBits : conceptBits) & conceptBitsInRequest;
    }
}