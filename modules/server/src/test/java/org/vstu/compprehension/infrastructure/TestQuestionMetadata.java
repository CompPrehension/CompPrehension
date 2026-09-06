package org.vstu.compprehension.infrastructure;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.data.QuestionMetadataData;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

/**
 * Перенос метаданных банка между сущностью и данными — только для тестов.
 * <p>
 * Боевой код такого перехода не делает: метаданные приходят из
 * {@code QuestionBankDataRepository} уже данными. Тестам же удобно брать строки
 * напрямую репозиторием сущностей — им не нужно ни ограничение слоёв, ни гарантии
 * полноты выборки.
 */
public final class TestQuestionMetadata {

    private TestQuestionMetadata() {
    }

    public static @NotNull QuestionMetadataData toData(@NotNull QuestionMetadataEntity entity) {
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
        data.setData(entity.getQuestionData() == null ? null : entity.getQuestionData().getData());
        return data;
    }

    public static @NotNull QuestionMetadataEntity toEntity(@NotNull QuestionMetadataData data) {
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
