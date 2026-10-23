package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.mappers.Mapping;

import java.util.List;

public interface QuestionMapper extends Mapping {

    @NotNull QuestionData map(@NotNull QuestionEntity question,
                              @NotNull List<InteractionEntity> interactions);

    @NotNull QuestionData map(@NotNull QuestionData question,
                              @NotNull QuestionEntity entity);
}
