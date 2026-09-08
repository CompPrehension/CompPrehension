package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;

import org.vstu.compprehension.repositories.Facts;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
class QuestionEntityMapperImpl implements QuestionEntityMapper {

    @Override
    public @NotNull QuestionEntity map(@NotNull QuestionData question,
                                       @Nullable QuestionMetadataEntity metadata) {
        var entity = new QuestionEntity();
        entity.setAnswerObjects(new ArrayList<>());
        entity.setInteractions(new ArrayList<>());
        apply(question, metadata, entity);
        return entity;
    }

    @Override
    public void apply(@NotNull QuestionData question,
                      @Nullable QuestionMetadataEntity metadata,
                      @NotNull QuestionEntity destination) {
        destination.setQuestionType(question.getQuestionType());
        destination.setQuestionStatus(question.getQuestionStatus());
        destination.setQuestionText(question.getQuestionText());
        destination.setQuestionName(question.getQuestionName());
        destination.setQuestionDomainType(question.getQuestionDomainType());
        destination.setOptions(question.getOptions());
        destination.setTags(question.getTags() == null
                ? new ArrayList<>() : new ArrayList<>(question.getTags()));
        destination.setStatementFacts(Facts.copy(question.getStatementFacts()));
        destination.setSolutionFacts(Facts.copy(question.getSolutionFacts()));
        destination.setMetadata(metadata);
        applyAnswerObjects(question, destination);
    }

    /** Варианты ответа обновляются по месту: уже сохранённые узнаются по идентификатору. */
    private void applyAnswerObjects(@NotNull QuestionData data, @NotNull QuestionEntity target) {
        if (data.getAnswerObjects() == null) {
            return;
        }
        if (target.getAnswerObjects() == null) {
            target.setAnswerObjects(new ArrayList<>());
        }
        var existing = target.getAnswerObjects().stream()
                .filter(a -> a.getId() != null)
                .collect(Collectors.toMap(AnswerObjectEntity::getId, a -> a, (a, b) -> a));

        for (AnswerObjectData source : data.getAnswerObjects()) {
            var entity = source.getId() == null ? null : existing.get(source.getId());
            if (entity == null) {
                entity = new AnswerObjectEntity();
                entity.setQuestion(target);
                target.getAnswerObjects().add(entity);
            }
            entity.setAnswerId(source.getAnswerId());
            entity.setHyperText(source.getHyperText());
            entity.setDomainInfo(source.getDomainInfo());
            entity.setRightCol(source.isRightCol());
            entity.setConcept(source.getConcept());
        }
    }
}
