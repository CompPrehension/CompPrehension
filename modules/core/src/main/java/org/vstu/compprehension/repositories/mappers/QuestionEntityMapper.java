package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.mappers.Mapping;

/**
 * Вопрос в сторону базы.
 * <p>
 * Именно правка на месте, а не новая сущность: строка вопроса уже может быть под
 * управлением Hibernate, и подмена экземпляра стоила бы и идентичности, и dirty checking.
 * <p>
 * Метаданные приходят готовой ссылкой на строку, а не моделью: они заведены банком
 * заданий и уже существуют. У вопросов, заведённых не через банк, их нет.
 */
public interface QuestionEntityMapper extends Mapping {

    /** Новая строка вопроса: коллекции заводятся пустыми, остальное переносит {@link #apply}. */
    @NotNull QuestionEntity map(@NotNull QuestionData question,
                                @Nullable QuestionMetadataEntity metadata);

    void apply(@NotNull QuestionData question,
               @Nullable QuestionMetadataEntity metadata,
               @NotNull QuestionEntity destination);
}
