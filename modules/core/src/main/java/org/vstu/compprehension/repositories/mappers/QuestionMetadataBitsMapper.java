package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionMetadataBitsData;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.mappers.Mapper;

/** Битовые маски метаданных вопроса — то, по чему считается качество подбора. */
@Component
class QuestionMetadataBitsMapper implements Mapper<QuestionMetadataEntity, QuestionMetadataBitsData> {

    @Override
    public @NotNull QuestionMetadataBitsData map(@NotNull QuestionMetadataEntity source) {
        return new QuestionMetadataBitsData(
                source.getId(),
                source.traceConceptsSatisfiedFromPlan(),
                source.traceConceptsUnsatisfiedFromPlan(),
                source.traceConceptsSatisfiedFromRequest(),
                source.getConceptBitsInRequest(),
                source.violationsSatisfiedFromPlan(),
                source.violationsUnsatisfiedFromPlan(),
                source.violationsSatisfiedFromRequest(),
                source.getViolationBitsInRequest(),
                source.getSkillBits());
    }
}
