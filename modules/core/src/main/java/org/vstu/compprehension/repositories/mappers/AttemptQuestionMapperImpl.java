package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.data.question.QuestionMetadataBitsData;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AttemptQuestionMapperImpl implements AttemptQuestionMapper {

    private final Mapper<QuestionMetadataEntity, QuestionMetadataBitsData> questionMetadataBitsMapper;

    @Override
    public @NotNull AttemptQuestionData map(@NotNull QuestionEntity question,
                                            @NotNull List<AttemptQuestionInteractionData> interactions) {
        // Метаданных нет у вопросов, заведённых не через банк заданий.
        var bits = Optional.ofNullable(question.getMetadata())
                .map(questionMetadataBitsMapper::map)
                .orElse(null);
        return new AttemptQuestionData(
                question.getId(),
                question.getQuestionName(),
                question.getQuestionDomainType(),
                bits,
                interactions);
    }
}
