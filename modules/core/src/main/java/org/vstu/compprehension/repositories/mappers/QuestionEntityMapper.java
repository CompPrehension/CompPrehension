package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.mappers.Mapping;

public interface QuestionEntityMapper extends Mapping {

    @NotNull QuestionEntity map(@NotNull QuestionData question,
                                @Nullable QuestionMetadataEntity metadata);

    void apply(@NotNull QuestionData question,
               @Nullable QuestionMetadataEntity metadata,
               @NotNull QuestionEntity destination);
}
