package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.repositories.entity.InteractionRepository.InteractionRow;

import java.util.List;

@Component
class AttemptQuestionInteractionMapperImpl implements AttemptQuestionInteractionMapper {

    @Override
    public @NotNull AttemptQuestionInteractionData map(@NotNull InteractionRow row,
                                                       @NotNull List<String> violationLawNames,
                                                       @NotNull List<String> correctLawNames) {
        return new AttemptQuestionInteractionData(
                row.getInteractionId(),
                row.getOrderNumber() == null ? 0 : row.getOrderNumber(),
                row.getInteractionType(),
                row.getInteractionsLeft(),
                violationLawNames,
                correctLawNames);
    }
}
