package org.vstu.compprehension.models.entities;

import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.Optional;

/**
 * In essence, a copy of class {@link QuestionMetadataEntity} that maps to a different table (it seems no other way around to store similar data in different table). Method fromMetadataEntity creates a new instance of this class from existing instance of QuestionMetadataEntity class.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@Entity
@Table(name = "questions_meta_draft")
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMetadataDraftEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /**
     * URL or name of GitHub repository from which this question was created
     */
    @Column(name = "origin")
    private String origin;

    @Column(name = "name")
    private String name;

    @Column(name = "domain_shortname")
    private String domainShortname;

    @Column(name = "template_id", nullable = false)
    private Integer templateId;

    @Column(name = "qt_graph")
    private String qtGraphPath;

    @Column(name = "qt_s_graph")
    private String qtSolvedGraphPath;

    @Column(name = "q_graph")
    private String qGraphPath;

    @Column(name = "q_s_graph")
    private String qSolvedGraphPath;

    @Column(name = "q_data_graph")
    private String qDataGraphPath;

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

    /** compact representation of meaningful structure; used to determine similar questions
     * */
    @Column(name = "structure_hash")
    private String structureHash;


    @Transient
    @Builder.Default
    private boolean isDraft = true;



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


    public QuestionMetadataEntity toMetadataEntity() {
        return QuestionMetadataEntity.builder()
                .id(null)  // (this.getId())
                .name(this.getName())
                .domainShortname(this.getDomainShortname())
                .templateId(Optional.ofNullable(this.getTemplateId()).orElse(-1))  // safe copy of NOT NULL field
                // ... don't set q_graph (it's useless in production)
                .qDataGraph(this.getQDataGraphPath())
                .tagBits(this.getTagBits())
                .conceptBits(this.getConceptBits())
                .lawBits(this.getLawBits())
                .violationBits(this.getViolationBits())
                .traceConceptBits(this.getTraceConceptBits())
                .solutionStructuralComplexity(this.getSolutionStructuralComplexity())
                .integralComplexity(this.getIntegralComplexity())
                .solutionSteps(this.getSolutionSteps())
                .distinctErrorsCount(this.getDistinctErrorsCount())
                .stage(this.getStage())
                .version(this.getVersion())
                .usedCount(this.getUsedCount())
                .dateLastUsed(this.getDateLastUsed())
                .lastAttemptId(this.getLastAttemptId())
                .structureHash(this.getStructureHash())
                // transient fields:
                .isDraft(false)
                /*.conceptBitsInPlan(other.getConceptBitsInPlan())*/
                /*.conceptBitsInRequest(other.getConceptBitsInRequest())*/
                /*.violationBitsInPlan(other.getViolationBitsInPlan())*/
                /*.violationBitsInRequest(other.getViolationBitsInRequest())*/
                .build();

    }

    public static QuestionMetadataDraftEntity fromMetadataEntity(QuestionMetadataEntity other) {
        return QuestionMetadataDraftEntity.builder()
                .id(null)  // (other.getId())
                .name(other.getName())
                .domainShortname(other.getDomainShortname())
                .templateId(Optional.ofNullable(other.getTemplateId()).orElse(-1))  // safe copy of NOT NULL field
                .qDataGraphPath(other.getQDataGraph())
                .tagBits(other.getTagBits())
                .conceptBits(other.getConceptBits())
                .lawBits(other.getLawBits())
                .violationBits(other.getViolationBits())
                .traceConceptBits(other.getTraceConceptBits())
                .solutionStructuralComplexity(other.getSolutionStructuralComplexity())
                .integralComplexity(other.getIntegralComplexity())
                .solutionSteps(other.getSolutionSteps())
                .distinctErrorsCount(other.getDistinctErrorsCount())
                .stage(other.getStage())
                .version(other.getVersion())
                .usedCount(other.getUsedCount())
                .dateLastUsed(other.getDateLastUsed())
                .lastAttemptId(other.getLastAttemptId())
                .structureHash(other.getStructureHash())
                // transient fields:
                .isDraft(true)  // other.isDraft()
                /*.conceptBitsInPlan(other.getConceptBitsInPlan())*/
                /*.conceptBitsInRequest(other.getConceptBitsInRequest())*/
                /*.violationBitsInPlan(other.getViolationBitsInPlan())*/
                /*.violationBitsInRequest(other.getViolationBitsInRequest())*/
                .build();
    }

}
