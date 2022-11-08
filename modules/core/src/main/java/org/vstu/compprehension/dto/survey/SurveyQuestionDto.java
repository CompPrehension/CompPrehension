package org.vstu.compprehension.dto.survey;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

@Value
@Jacksonized
@Builder
public class SurveyQuestionDto {
    @NotNull Long id;
    @NotNull String type;
    @NotNull Object policy;
    boolean required;
    @NotNull String text;
    @NotNull Object options;
}
