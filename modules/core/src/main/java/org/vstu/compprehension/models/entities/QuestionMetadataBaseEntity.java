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


    default double complexityAbsDiff(double complexity) {
        return Math.abs(getIntegralComplexity() - complexity);
    }
    default double getSolutionStepsAbsDiff(double steps) {
        return Math.abs(getSolutionSteps() - steps);
    }
}
