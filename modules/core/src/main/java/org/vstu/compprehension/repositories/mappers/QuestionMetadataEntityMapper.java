package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.mappers.Mapper;

/** Метаданные вопроса в сторону базы; тело сохраняется отдельно и здесь не трогается. */
@Component
class QuestionMetadataEntityMapper implements Mapper<QuestionMetadataData, QuestionMetadataEntity> {

    @Override
    public @NotNull QuestionMetadataEntity map(@NotNull QuestionMetadataData source) {
        return QuestionMetadataEntity.builder()
                .id(source.getId())
                .name(source.getName())
                .domainShortname(source.getDomainShortname())
                .templateId(source.getTemplateId())
                .qDataGraph(source.getQDataGraph())
                .tagBits(source.getTagBits())
                .conceptBits(source.getConceptBits())
                .lawBits(source.getLawBits())
                .skillBits(source.getSkillBits())
                .violationBits(source.getViolationBits())
                .traceConceptBits(source.getTraceConceptBits())
                .solutionStructuralComplexity(source.getSolutionStructuralComplexity())
                .integralComplexity(source.getIntegralComplexity())
                .solutionSteps(source.getSolutionSteps())
                .distinctErrorsCount(source.getDistinctErrorsCount())
                .version(source.getVersion())
                .structureHash(source.getStructureHash())
                .origin(source.getOrigin())
                .originLicense(source.getOriginLicense())
                .createdAt(source.getCreatedAt())
                .generationRequestId(source.getGenerationRequestId())
                .build();
    }
}
