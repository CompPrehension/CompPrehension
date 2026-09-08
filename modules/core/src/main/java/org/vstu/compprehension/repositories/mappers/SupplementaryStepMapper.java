package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.SupplementaryStepRepository.StepRow;

/** Шаг цепочки вспомогательных вопросов. */
@Component
class SupplementaryStepMapper implements Mapper<StepRow, SupplementaryStepData> {

    @Override
    public @NotNull SupplementaryStepData map(@NotNull StepRow source) {
        var entity = source.getStep();
        return SupplementaryStepData.builder()
                .id(entity.getId())
                .mainQuestionInteractionId(source.getMainQuestionInteractionId())
                .situationInfo(entity.getSituationInfo())
                .nextStateId(entity.getNextStateId())
                .build();
    }
}
