package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.mappers.UpdateMapper;
import org.vstu.compprehension.utils.Strict;

import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
class QuestionMetadataWithDataMapper implements Mapper<QuestionMetadataEntity, QuestionMetadataWithData> {

    private final UpdateMapper<QuestionMetadataEntity, QuestionMetadataData> questionMetadataMapper;

    @Override
    public @NotNull QuestionMetadataWithData map(@NotNull QuestionMetadataEntity source) {

        var questionData = source.getQuestionData();
        if (questionData == null || questionData.getData() == null) {
            throw new NoSuchElementException(
                    "Question body is not loaded for metadata " + source.getId());
        }

        var destination = new QuestionMetadataWithData();
        questionMetadataMapper.apply(source, destination);
        destination.setData(questionData.getData());
        
        return destination;
    }
}
