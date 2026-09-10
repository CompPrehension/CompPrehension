package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.mappers.Mapping;

public interface QuestionDtoMapper extends Mapping {

    @NotNull QuestionDto map(@NotNull QuestionData question, @NotNull Language language);
}
