package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exerciseattempt.AttemptExerciseData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.mappers.Mapper;

/** Вариант ответа. */
@Component
class AnswerObjectMapper implements Mapper<AnswerObjectEntity, AnswerObjectData> {

    @Override
    public @NotNull AnswerObjectData map(@NotNull AnswerObjectEntity source) {
        return AnswerObjectData.builder()
                .id(source.getId())
                .answerId(source.getAnswerId())
                .hyperText(source.getHyperText())
                .domainInfo(source.getDomainInfo())
                .isRightCol(source.isRightCol())
                .concept(source.getConcept())
                .build();
    }
}
