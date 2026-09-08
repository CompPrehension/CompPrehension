package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.survey.SurveyQuestionData;
import org.vstu.compprehension.entities.SurveyQuestionEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;

@Component
class SurveyQuestionMapper implements Mapper<SurveyQuestionEntity, SurveyQuestionData> {

    @Override
    public @NotNull SurveyQuestionData map(@NotNull SurveyQuestionEntity source) {
        long id = Strict.required(source.getId(), "id", "survey question");
        String owner = "survey question " + id;
        return new SurveyQuestionData(
                id,
                Strict.required(source.getType(), "type", owner),
                Strict.required(source.getText(), "text", owner),
                source.isRequired(),
                Strict.required(source.getPolicy(), "policy", owner),
                Strict.required(source.getOptions(), "options", owner));
    }
}
