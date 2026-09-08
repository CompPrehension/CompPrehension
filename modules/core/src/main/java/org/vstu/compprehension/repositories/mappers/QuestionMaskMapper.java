package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionMaskData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.QuestionMetadataRepository.QuestionMaskView;

/** Маски вопроса; незаполненная маска читается как ноль. */
@Component
class QuestionMaskMapper implements Mapper<QuestionMaskView, QuestionMaskData> {

    @Override
    public @NotNull QuestionMaskData map(@NotNull QuestionMaskView source) {
        return new QuestionMaskData(
                source.getConceptBits() == null ? 0L : source.getConceptBits(),
                source.getLawBits() == null ? 0L : source.getLawBits(),
                source.getViolationBits() == null ? 0L : source.getViolationBits(),
                source.getSkillBits() == null ? 0L : source.getSkillBits());
    }
}
