package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.common.Utils;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.mappers.Mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class QuestionMapperImpl implements QuestionMapper {

    private final Mapper<QuestionMetadataEntity, QuestionMetadataData> questionMetadataMapper;
    private final Mapper<AnswerObjectEntity, AnswerObjectData> answerObjectMapper;
    private final Mapper<InteractionEntity, QuestionInteractionData> questionInteractionMapper;

    @Override
    public @NotNull QuestionData map(@NotNull QuestionEntity question,
                                     @NotNull List<InteractionEntity> interactions) {
        var data = new QuestionData();
        data.setId(question.getId());
        data.setQuestionType(question.getQuestionType());
        data.setQuestionStatus(question.getQuestionStatus());
        data.setQuestionText(question.getQuestionText());
        data.setQuestionName(question.getQuestionName());
        data.setCreatedAt(question.getCreatedAt());
        data.setQuestionDomainType(question.getQuestionDomainType());
        data.setOptions(question.getOptions());
        data.setTags(new ArrayList<>(question.getTags()));
        // Метаданных нет у вопросов, заведённых не через банк заданий.
        data.setMetadata(Optional.ofNullable(question.getMetadata())
                .map(questionMetadataMapper::map)
                .orElse(null));
        data.setStatementFacts(Utils.copy(question.getStatementFacts()));
        data.setSolutionFacts(Utils.copy(question.getSolutionFacts()));
        data.setAnswerObjects(question.getAnswerObjects().stream()
                .map(answerObjectMapper::map)
                .collect(Collectors.toCollection(ArrayList::new)));

        var interactionsData = interactions.stream()
                .sorted(Comparator.comparing(InteractionEntity::getId))
                .map(questionInteractionMapper::map)
                .collect(Collectors.toCollection(ArrayList::new));
        data.setInteractions(interactionsData);
        return data;
    }
}
