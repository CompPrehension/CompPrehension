package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.survey.SurveyData;
import org.vstu.compprehension.data.survey.SurveyOptionsData;
import org.vstu.compprehension.data.survey.SurveyQuestionData;
import org.vstu.compprehension.entities.SurveyEntity;
import org.vstu.compprehension.entities.SurveyQuestionEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;

@Component
@RequiredArgsConstructor
class SurveyMapper implements Mapper<SurveyEntity, SurveyData> {

    private final Mapper<SurveyQuestionEntity, SurveyQuestionData> surveyQuestionMapper;

    @Override
    public @NotNull SurveyData map(@NotNull SurveyEntity source) {
        String surveyId = Strict.required(source.getSurveyId(), "surveyId", "survey");
        String owner = "survey " + surveyId;
        var options = Strict.required(source.getOptions(), "options", owner);
        return new SurveyData(
                surveyId,
                new SurveyOptionsData(Strict.required(options.getSize(), "options.size", owner)),
                surveyQuestionMapper.mapAll(
                        Strict.required(source.getQuestions(), "questions", owner)));
    }
}
