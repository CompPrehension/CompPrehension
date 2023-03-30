package org.vstu.compprehension.models.entities;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Getter @Setter
@Builder
@Entity
@Table(name = "questions_meta")
@NoArgsConstructor
@AllArgsConstructor
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

    /** 3: ready for usage (or import, i.e. created),
     *  4: imported,
     *  other: invalid */
    @Column(name = "_stage")
    private Integer stage;

    @Column(name = "_version")
    private Integer version;

    @Column(name = "used_count")
    private Long usedCount;

    @Column(name = "date_last_used")
    private Date dateLastUsed;

    @Column(name = "last_attempt_id")
    private Long lastAttemptId;

    /** compact representation of meaningful structure; may be used to determine similar questions
     * */
    @Column(name = "structure_hash")
    private String structureHash;


    @Transient
    @Builder.Default
    private boolean isDraft = false;


    @Transient
    @Builder.Default
    private Long conceptBitsInPlan = 0L; // planned by exercise
    @Transient
    @Builder.Default
    private Long conceptBitsInRequest = 0L; // actually requested

    @Transient
    @Builder.Default
    private Long violationBitsInPlan = 0L; // planned by exercise
    @Transient
    @Builder.Default
    private Long violationBitsInRequest = 0L; // actually requested


    public double complexityAbsDiff(double complexity) {
        return Math.abs(getIntegralComplexity() - complexity);
    }
    public double getSolutionStepsAbsDiff(double steps) {
        return Math.abs(getSolutionSteps() - steps);

    }

    /** Common bits of concepts in plan and concepts (or trace concepts) in question */
    public Long traceConceptsSatisfiedFromPlan() {
        return (traceConceptBits != 0 ? traceConceptBits : conceptBits) & conceptBitsInPlan;  // traceConceptBits are not applicable to all domains...
    }
    /** Concepts from plan absent in question's concepts (or trace concepts) */
    public Long traceConceptsUnsatisfiedFromPlan() {
        return ~(traceConceptBits != 0 ? traceConceptBits : conceptBits) & conceptBitsInPlan;
    }

    /** Common bits of violations in plan and violations in question */
    public Long violationsSatisfiedFromPlan() {
        return violationBits & violationBitsInPlan;
    }
    /** Violations from plan absent in question's violations */
    public Long violationsUnsatisfiedFromPlan() {
        return ~violationBits & violationBitsInPlan;
    }


    /** Common bits of concepts in request and concepts (or trace concepts) in question */
    public Long traceConceptsSatisfiedFromRequest() {
        return (traceConceptBits != 0 ? traceConceptBits : conceptBits) & conceptBitsInRequest;  // traceConceptBits are not applicable to all domains...
    }
    /** Concepts from request absent in question's concepts (or trace concepts) */
    public Long traceConceptsUnsatisfiedFromRequest() {
        return ~(traceConceptBits != 0 ? traceConceptBits : conceptBits) & conceptBitsInRequest;
    }

    /** Common bits of violations in request and violations in question */
    public Long violationsSatisfiedFromRequest() {
        return violationBits & violationBitsInRequest;
    }
    /** Violations from request absent in question's violations */
    public Long violationsUnsatisfiedFromRequest() {
        return ~violationBits & violationBitsInRequest;
    }
}