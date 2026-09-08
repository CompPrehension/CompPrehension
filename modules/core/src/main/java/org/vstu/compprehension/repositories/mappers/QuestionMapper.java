package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.mappers.Mapping;

import java.util.List;

/**
 * Вопрос целиком — то, с чем работает бизнес-логика.
 * <p>
 * Взаимодействия приходят отдельным списком, потому что подгружаются несколькими
 * запросами: вместе с нарушениями, ответами и подтверждёнными законами. Сам вопрос
 * обязан прийти с метаданными и вариантами ответа.
 */
public interface QuestionMapper extends Mapping {

    @NotNull QuestionData map(@NotNull QuestionEntity question,
                              @NotNull List<InteractionEntity> interactions);
}
