package org.vstu.compprehension.data.question;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.QuestionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class QuestionData {
    @Nullable Long id;
    @Nullable QuestionStatus questionStatus;
    @Nullable Date createdAt;
    @NotNull QuestionContentData content;
    @NotNull List<QuestionInteractionData> interactions;

    @Builder(toBuilder = true)
    public QuestionData(@Nullable Long id,
                        @Nullable QuestionStatus questionStatus,
                        @Nullable Date createdAt,
                        @NotNull QuestionContentData content,
                        @Nullable List<QuestionInteractionData> interactions) {
        this.id = id;
        this.questionStatus = questionStatus;
        this.createdAt = createdAt;
        this.content = Objects.requireNonNull(content, "content");
        this.interactions = interactions == null || interactions.isEmpty()
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(interactions));
    }

    public static @NotNull QuestionData of(@NotNull QuestionContentData content) {
        return new QuestionData(null, null, null, content, null);
    }

    public @NotNull QuestionData withContent(@NotNull QuestionContentData newContent) {
        return new QuestionData(id, questionStatus, createdAt, newContent, interactions);
    }

    public @NotNull QuestionData withInteraction(@NotNull QuestionInteractionData interaction) {
        Set<Long> movedResponseIds = interaction.getResponses().stream()
                .map(ResponseData::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var updated = new ArrayList<QuestionInteractionData>(interactions.size() + 1);
        for (QuestionInteractionData previous : interactions) {
            updated.add(withoutResponses(previous, movedResponseIds));
        }
        updated.add(interaction);
        return new QuestionData(id, questionStatus, createdAt, content, updated);
    }

    public @NotNull Optional<QuestionInteractionData> latestCorrectInteraction() {
        return interactions.stream()
                .filter(QuestionInteractionData::isCorrect)
                .filter(QuestionInteractionData::allowsMoreSteps)
                .reduce((first, second) -> second);
    }

    /** Ответы последнего верного взаимодействия; пусто, если верных взаимодействий ещё не было. */
    public @NotNull List<ResponseData> latestCorrectResponses() {
        return latestCorrectInteraction()
                .map(QuestionInteractionData::getResponses)
                .orElseGet(List::of);
    }

    public int correctInteractionsCount() {
        return (int) interactions.stream().filter(QuestionInteractionData::isCorrect).count();
    }

    public int erroneousInteractionsCount() {
        return interactions.size() - correctInteractionsCount();
    }

    private static @NotNull QuestionInteractionData withoutResponses(@NotNull QuestionInteractionData interaction,
                                                                    @NotNull Set<Long> responseIds) {
        if (responseIds.isEmpty() || interaction.getResponses().stream()
                .noneMatch(response -> responseIds.contains(response.getId()))) {
            return interaction;
        }
        return interaction.toBuilder()
                .responses(interaction.getResponses().stream()
                        .filter(response -> !responseIds.contains(response.getId()))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .build();
    }
}
