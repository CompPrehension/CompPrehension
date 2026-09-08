package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.survey.SurveyVoteData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.Strict;
import org.vstu.compprehension.repositories.entity.SurveyRepository.SurveyVoteView;

/** Голос студента в опросе. */
@Component
class SurveyVoteMapper implements Mapper<SurveyVoteView, SurveyVoteData> {

    @Override
    public @NotNull SurveyVoteData map(@NotNull SurveyVoteView source) {
        long surveyQuestionId = Strict.required(
                source.getSurveyQuestionId(), "surveyQuestionId", "survey vote");
        String owner = "vote on survey question " + surveyQuestionId;
        return new SurveyVoteData(
                surveyQuestionId,
                Strict.required(source.getQuestionId(), "questionId", owner),
                source.getAnswer());
    }
}
