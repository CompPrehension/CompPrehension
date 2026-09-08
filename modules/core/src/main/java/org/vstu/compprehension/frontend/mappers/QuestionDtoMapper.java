package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.Question;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.mappers.Mapping;

public interface QuestionDtoMapper extends Mapping {

    @NotNull QuestionDto map(@NotNull Question question, @NotNull Language language);
}
