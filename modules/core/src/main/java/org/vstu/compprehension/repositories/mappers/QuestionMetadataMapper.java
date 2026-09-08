package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.mappers.Mapper;

@Component
class QuestionMetadataMapper implements Mapper<QuestionMetadataEntity, QuestionMetadataData> {

    @Override
    public @NotNull QuestionMetadataData map(@NotNull QuestionMetadataEntity source) {
        var data = new QuestionMetadataData();
        data.setId(source.getId());
        data.setName(source.getName());
        data.setDomainShortname(source.getDomainShortname());
        data.setTemplateId(source.getTemplateId());
        data.setQDataGraph(source.getQDataGraph());
        data.setTagBits(source.getTagBits());
        data.setConceptBits(source.getConceptBits());
        data.setLawBits(source.getLawBits());
        data.setSkillBits(source.getSkillBits());
        data.setViolationBits(source.getViolationBits());
        data.setTraceConceptBits(source.getTraceConceptBits());
        data.setSolutionStructuralComplexity(source.getSolutionStructuralComplexity());
        data.setIntegralComplexity(source.getIntegralComplexity());
        data.setSolutionSteps(source.getSolutionSteps());
        data.setDistinctErrorsCount(source.getDistinctErrorsCount());
        data.setVersion(source.getVersion());
        data.setStructureHash(source.getStructureHash());
        data.setOrigin(source.getOrigin());
        data.setOriginLicense(source.getOriginLicense());
        data.setCreatedAt(source.getCreatedAt());
        data.setGenerationRequestId(source.getGenerationRequestId());
        data.setConceptBitsInPlan(source.getConceptBitsInPlan());
        data.setConceptBitsInRequest(source.getConceptBitsInRequest());
        data.setViolationBitsInPlan(source.getViolationBitsInPlan());
        data.setViolationBitsInRequest(source.getViolationBitsInRequest());
        data.setSkillBitsInPlan(source.getSkillBitsInPlan());
        data.setData(source.getQuestionData() == null ? null : source.getQuestionData().getData());
        return data;
    }
}
