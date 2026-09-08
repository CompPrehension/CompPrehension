package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.mappers.Mapping;
import org.vstu.compprehension.repositories.entity.InteractionRepository.InteractionRow;

import java.util.List;

public interface AttemptQuestionInteractionMapper extends Mapping {

    @NotNull AttemptQuestionInteractionData map(@NotNull InteractionRow row,
                                                @NotNull List<String> violationLawNames,
                                                @NotNull List<String> correctLawNames);
}
