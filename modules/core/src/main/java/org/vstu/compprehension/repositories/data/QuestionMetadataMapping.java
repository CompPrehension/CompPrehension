package org.vstu.compprehension.repositories.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.entities.QuestionMetadataEntity;


final class QuestionMetadataMapping {

    private QuestionMetadataMapping() {
    }

    static @Nullable QuestionMetadataData toData(@Nullable QuestionMetadataEntity entity) {
        if (entity == null) {
            return null;
        }
        var data = new QuestionMetadataData();
        data.setId(entity.getId());
        data.setName(entity.getName());
        data.setDomainShortname(entity.getDomainShortname());
        data.setTemplateId(entity.getTemplateId());
        data.setQDataGraph(entity.getQDataGraph());
        data.setTagBits(entity.getTagBits());
        data.setConceptBits(entity.getConceptBits());
        data.setLawBits(entity.getLawBits());
        data.setSkillBits(entity.getSkillBits());
        data.setViolationBits(entity.getViolationBits());
        data.setTraceConceptBits(entity.getTraceConceptBits());
        data.setSolutionStructuralComplexity(entity.getSolutionStructuralComplexity());
        data.setIntegralComplexity(entity.getIntegralComplexity());
        data.setSolutionSteps(entity.getSolutionSteps());
        data.setDistinctErrorsCount(entity.getDistinctErrorsCount());
        data.setVersion(entity.getVersion());
        data.setStructureHash(entity.getStructureHash());
        data.setOrigin(entity.getOrigin());
        data.setOriginLicense(entity.getOriginLicense());
        data.setCreatedAt(entity.getCreatedAt());
        data.setGenerationRequestId(entity.getGenerationRequestId());
        data.setConceptBitsInPlan(entity.getConceptBitsInPlan());
        data.setConceptBitsInRequest(entity.getConceptBitsInRequest());
        data.setViolationBitsInPlan(entity.getViolationBitsInPlan());
        data.setViolationBitsInRequest(entity.getViolationBitsInRequest());
        data.setSkillBitsInPlan(entity.getSkillBitsInPlan());
        data.setData(entity.getQuestionData() == null ? null : entity.getQuestionData().getData());
        return data;
    }

    static @NotNull QuestionMetadataEntity toEntity(@NotNull QuestionMetadataData data) {
        return QuestionMetadataEntity.builder()
                .id(data.getId())
                .name(data.getName())
                .domainShortname(data.getDomainShortname())
                .templateId(data.getTemplateId())
                .qDataGraph(data.getQDataGraph())
                .tagBits(data.getTagBits())
                .conceptBits(data.getConceptBits())
                .lawBits(data.getLawBits())
                .skillBits(data.getSkillBits())
                .violationBits(data.getViolationBits())
                .traceConceptBits(data.getTraceConceptBits())
                .solutionStructuralComplexity(data.getSolutionStructuralComplexity())
                .integralComplexity(data.getIntegralComplexity())
                .solutionSteps(data.getSolutionSteps())
                .distinctErrorsCount(data.getDistinctErrorsCount())
                .version(data.getVersion())
                .structureHash(data.getStructureHash())
                .origin(data.getOrigin())
                .originLicense(data.getOriginLicense())
                .createdAt(data.getCreatedAt())
                .generationRequestId(data.getGenerationRequestId())
                .build();
    }
}
