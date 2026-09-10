package org.vstu.compprehension.data.question;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Вопрос, сгенерированный доменом.
 */
@Value
public class GeneratedQuestionData {
    @NotNull QuestionContentData content;
    @NotNull List<String> concepts;
    @NotNull List<String> negativeLaws;

    @Builder(toBuilder = true)
    public GeneratedQuestionData(@NotNull QuestionContentData content,
                                 @Nullable List<String> concepts,
                                 @Nullable List<String> negativeLaws) {
        this.content = Objects.requireNonNull(content, "content");
        this.concepts = frozen(concepts);
        this.negativeLaws = frozen(negativeLaws);
    }

    public static @NotNull GeneratedQuestionData of(@NotNull QuestionContentData content) {
        return new GeneratedQuestionData(content, null, null);
    }

    public @NotNull GeneratedQuestionData withContent(@NotNull QuestionContentData newContent) {
        return new GeneratedQuestionData(newContent, concepts, negativeLaws);
    }

    private static <T> @NotNull List<T> frozen(@Nullable List<T> values) {
        return values == null || values.isEmpty()
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
