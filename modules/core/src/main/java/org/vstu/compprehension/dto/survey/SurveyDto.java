package org.vstu.compprehension.dto.survey;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.data.SurveyOptionsData;

@Value
@Jacksonized
@Builder
public class SurveyDto {
    @NotNull String surveyId;
    @NotNull SurveyOptionsData options;
    @Builder.Default
    @NotNull SurveyQuestionDto[] questions = new SurveyQuestionDto[0];
}
