package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.survey.SurveyVoteData;
import org.vstu.compprehension.frontend.dto.survey.SurveyResultDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class SurveyResultDtoMapper implements Mapper<SurveyVoteData, SurveyResultDto> {

    @Override
    public @NotNull SurveyResultDto map(@NotNull SurveyVoteData source) {
        return SurveyResultDto.builder()
                .surveyQuestionId(source.surveyQuestionId())
                .questionId(source.questionId())
                .answer(source.answer())
                .build();
    }
}
