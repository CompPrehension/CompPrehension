package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.survey.SurveyData;
import org.vstu.compprehension.data.survey.SurveyQuestionData;
import org.vstu.compprehension.frontend.dto.survey.SurveyDto;
import org.vstu.compprehension.frontend.dto.survey.SurveyQuestionDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
@RequiredArgsConstructor
class SurveyDtoMapper implements Mapper<SurveyData, SurveyDto> {

    @Override
    public @NotNull SurveyDto map(@NotNull SurveyData source) {
        return SurveyDto.builder()
                .surveyId(source.surveyId())
                .options(source.options())
                .questions(source.questions()
                        .stream().map(this::map)
                        .toArray(SurveyQuestionDto[]::new))
                .build();
    }

    private @NotNull SurveyQuestionDto map(@NotNull SurveyQuestionData source) {
        return SurveyQuestionDto.builder()
                .id(source.id())
                .type(source.type())
                .text(source.text())
                .required(source.required())
                .policy(source.policy())
                .options(source.options())
                .build();
    }
}
