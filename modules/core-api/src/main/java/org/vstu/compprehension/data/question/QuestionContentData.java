package org.vstu.compprehension.data.question;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.questionoptions.QuestionOptionsData;
import org.vstu.compprehension.enums.QuestionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Содержимое вопроса, сгенерированного доменом.
 */
@Value
public class QuestionContentData {
    @NotNull String domainId;
    @Nullable QuestionType questionType;
    @Nullable String questionText;
    @Nullable String questionName;
    @Nullable String questionDomainType;
    @Nullable QuestionOptionsData options;
    @NotNull List<AnswerObjectData> answerObjects;
    @NotNull List<BackendFactData> statementFacts;
    @NotNull List<BackendFactData> solutionFacts;
    @NotNull List<String> tags;
    @Nullable QuestionMetadataData metadata;

    @Builder(toBuilder = true)
    public QuestionContentData(@NotNull String domainId,
                               @Nullable QuestionType questionType,
                               @Nullable String questionText,
                               @Nullable String questionName,
                               @Nullable String questionDomainType,
                               @Nullable QuestionOptionsData options,
                               @Nullable List<AnswerObjectData> answerObjects,
                               @Nullable List<BackendFactData> statementFacts,
                               @Nullable List<BackendFactData> solutionFacts,
                               @Nullable List<String> tags,
                               @Nullable QuestionMetadataData metadata) {
        this.domainId = Objects.requireNonNull(domainId, "domainId");
        this.questionType = questionType;
        this.questionText = questionText;
        this.questionName = questionName;
        this.questionDomainType = questionDomainType;
        this.options = options;
        this.answerObjects = frozen(answerObjects);
        this.statementFacts = frozen(statementFacts);
        this.solutionFacts = frozen(solutionFacts);
        this.tags = frozen(tags);
        this.metadata = metadata;
    }

    public @Nullable AnswerObjectData getAnswerObject(int answerId) {
        for (AnswerObjectData a : answerObjects) {
            if (a.getAnswerId() != null && a.getAnswerId() == answerId) {
                return a;
            }
        }
        return null;
    }

    public boolean isSupplementary() {
        return questionDomainType != null && questionDomainType.contains("Supplementary");
    }

    private static <T> @NotNull List<T> frozen(@Nullable List<T> values) {
        return values == null || values.isEmpty()
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
