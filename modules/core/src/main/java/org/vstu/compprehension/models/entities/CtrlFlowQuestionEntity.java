package org.vstu.compprehension.models.entities;

import javax.persistence.*;

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getDistinctErrorsCount() {
        return distinctErrorsCount;
    }

    public void setDistinctErrorsCount(Integer distinctErrorsCount) {
        this.distinctErrorsCount = distinctErrorsCount;
    }

    public Integer getSolutionSteps() {
        return solutionSteps;
    }

    public void setSolutionSteps(Integer solutionSteps) {
        this.solutionSteps = solutionSteps;
    }

    public Double getIntegralComplexity() {
        return integralComplexity;
    }

    public void setIntegralComplexity(Double integralComplexity) {
        this.integralComplexity = integralComplexity;
    }

    public Double getSolutionStructuralComplexity() {
        return solutionStructuralComplexity;
    }

    public void setSolutionStructuralComplexity(Double solutionStructuralComplexity) {
        this.solutionStructuralComplexity = solutionStructuralComplexity;
    }

    public Integer getViolationBits() {
        return violationBits;
    }

    public void setViolationBits(Integer violationBits) {
        this.violationBits = violationBits;
    }

    public Integer getLawBits() {
        return lawBits;
    }

    public void setLawBits(Integer lawBits) {
        this.lawBits = lawBits;
    }

    public Integer getConceptBits() {
        return conceptBits;
    }

    public void setConceptBits(Integer conceptBits) {
        this.conceptBits = conceptBits;
    }

    public Integer getTagBits() {
        return tagBits;
    }

    public void setTagBits(Integer tagBits) {
        this.tagBits = tagBits;
    }

    public String getQDataGraph() {
        return qDataGraph;
    }

    public void setQDataGraph(String qDataGraph) {
        this.qDataGraph = qDataGraph;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}