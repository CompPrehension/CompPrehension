package org.vstu.compprehension.models.entities;

public interface QuestionMetadataBaseEntity {

    Integer getStage();

    Integer getDistinctErrorsCount();

    Integer getSolutionSteps();

    Double getIntegralComplexity();

    Double getSolutionStructuralComplexity();

    Integer getViolationBits();

    Integer getLawBits();

    Integer getConceptBits();

    Integer getTagBits();

    String getQDataGraph();

    Integer getTemplateId();

    String getName();

    Integer getId();
}
