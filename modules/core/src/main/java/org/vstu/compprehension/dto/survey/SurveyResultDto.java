package org.vstu.compprehension.dto.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Value @AllArgsConstructor
public class SurveyResultDto {
    Long surveyQuestionId;
    Long questionId;
    String answer;
}
