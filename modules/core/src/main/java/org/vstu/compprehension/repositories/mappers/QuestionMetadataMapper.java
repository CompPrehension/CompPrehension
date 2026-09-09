package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.mappers.UpdateMapper;

@Component
class QuestionMetadataMapper implements Mapper<QuestionMetadataEntity, QuestionMetadataData>, UpdateMapper<QuestionMetadataEntity, QuestionMetadataData> {

    @Override
    public @NotNull QuestionMetadataData map(@NotNull QuestionMetadataEntity source) {
        var destination = new QuestionMetadataData();
        apply(source, destination);
        return destination;
    }

    @Override
    public void apply(@NotNull QuestionMetadataEntity source, @NotNull QuestionMetadataData destination) {
        destination.setId(source.getId());
        destination.setName(source.getName());
        destination.setDomainShortname(source.getDomainShortname());
        destination.setTemplateId(source.getTemplateId());
        destination.setQDataGraph(source.getQDataGraph());
        destination.setTagBits(source.getTagBits());
        destination.setConceptBits(source.getConceptBits());
        destination.setLawBits(source.getLawBits());
        destination.setSkillBits(source.getSkillBits());
        destination.setViolationBits(source.getViolationBits());
        destination.setTraceConceptBits(source.getTraceConceptBits());
        destination.setSolutionStructuralComplexity(source.getSolutionStructuralComplexity());
        destination.setIntegralComplexity(source.getIntegralComplexity());
        destination.setSolutionSteps(source.getSolutionSteps());
        destination.setDistinctErrorsCount(source.getDistinctErrorsCount());
        destination.setVersion(source.getVersion());
        destination.setStructureHash(source.getStructureHash());
        destination.setOrigin(source.getOrigin());
        destination.setOriginLicense(source.getOriginLicense());
        destination.setCreatedAt(source.getCreatedAt());
        destination.setGenerationRequestId(source.getGenerationRequestId());
        destination.setConceptBitsInPlan(source.getConceptBitsInPlan());
        destination.setConceptBitsInRequest(source.getConceptBitsInRequest());
        destination.setViolationBitsInPlan(source.getViolationBitsInPlan());
        destination.setViolationBitsInRequest(source.getViolationBitsInRequest());
        destination.setSkillBitsInPlan(source.getSkillBitsInPlan());
    }
}
