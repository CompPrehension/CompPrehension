package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.common.Utils;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionContentData;
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
        var content = QuestionContentData.builder()
                .domainId(question.getDomainEntity().getName())
                .questionType(question.getQuestionType())
                .questionText(question.getQuestionText())
                .questionName(question.getQuestionName())
                .questionDomainType(question.getQuestionDomainType())
                .options(question.getOptions())
                .tags(new ArrayList<>(question.getTags()))
                // Метаданных нет у вопросов, заведённых не через банк заданий.
                .metadata(Optional.ofNullable(question.getMetadata())
                        .map(questionMetadataMapper::map)
                        .orElse(null))
                .statementFacts(Utils.copy(question.getStatementFacts()))
                .solutionFacts(Utils.copy(question.getSolutionFacts()))
                .answerObjects(question.getAnswerObjects().stream()
                        .map(answerObjectMapper::map)
                        .toList())
                .build();

        var interactionsData = interactions.stream()
                .sorted(Comparator.comparing(InteractionEntity::getId))
                .map(questionInteractionMapper::map)
                .collect(Collectors.toCollection(ArrayList::new));

        return QuestionData.builder()
                .id(question.getId())
                .questionStatus(question.getQuestionStatus())
                .createdAt(question.getCreatedAt())
                .content(content)
                .interactions(interactionsData)
                .build();
    }

    @Override
    public @NotNull QuestionData map(@NotNull QuestionData question,
                                     @NotNull QuestionEntity entity) {
        var answerObjects = question.getContent().getAnswerObjects();
        var stored = entity.getAnswerObjects() == null ? List.<AnswerObjectEntity>of() : entity.getAnswerObjects();

        var withIds = new ArrayList<AnswerObjectData>(answerObjects.size());
        for (int i = 0; i < answerObjects.size(); i++) {
            var source = answerObjects.get(i);
            withIds.add(source.getId() != null || i >= stored.size()
                    ? source
                    : source.toBuilder().id(stored.get(i).getId()).build());
        }

        return question.toBuilder()
                .id(entity.getId())
                .content(question.getContent().toBuilder().answerObjects(withIds).build())
                .build();
    }
}
