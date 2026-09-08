package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.mappers.Mapping;

import java.util.List;

public interface AttemptQuestionMapper extends Mapping {

    @NotNull AttemptQuestionData map(@NotNull QuestionEntity question,
                                     @NotNull List<AttemptQuestionInteractionData> interactions);
}
