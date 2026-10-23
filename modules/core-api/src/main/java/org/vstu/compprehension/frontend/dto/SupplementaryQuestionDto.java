package org.vstu.compprehension.frontend.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
public sealed interface SupplementaryQuestionDto {
    @JsonTypeName("QUESTION")
    record Question(@NotNull QuestionDto question) implements SupplementaryQuestionDto {}

    @JsonTypeName("FEEDBACK")
    record Feedback(@NotNull SupplementaryFeedbackDto feedback) implements SupplementaryQuestionDto {}
}
