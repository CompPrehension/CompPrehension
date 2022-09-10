package org.vstu.compprehension.dto.survey;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Value
public class SurveyResultDto {
    Long surveyQuestionId;
    Long questionId;
    String answer;
}
