package org.vstu.compprehension.data.question;

import org.vstu.compprehension.data.questionoptions.QuestionOptionsData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.QuestionStatus;
import org.vstu.compprehension.enums.QuestionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class QuestionData {
    private Long id;
    private QuestionType questionType;
    private QuestionStatus questionStatus;
    private String questionText;
    private String questionName;
    private Date createdAt;
    private @Nullable QuestionMetadataData metadata;
    private String questionDomainType;
    private QuestionOptionsData options;
    private @NotNull List<String> tags = new ArrayList<>(0);
    private List<AnswerObjectData> answerObjects = new ArrayList<>();
    private @NotNull List<QuestionInteractionData> interactions = new ArrayList<>(0);
    private List<BackendFactData> statementFacts = new ArrayList<>();
    private List<BackendFactData> solutionFacts = new ArrayList<>();

    public void addInteraction(@NotNull QuestionInteractionData interaction) {
        Set<Long> movedResponseIds = interaction.getResponses().stream()
                .map(ResponseData::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (QuestionInteractionData previous : interactions) {
            previous.getResponses().removeIf(response -> movedResponseIds.contains(response.getId()));
        }
        interactions.add(interaction);
    }

    public @NotNull Optional<QuestionInteractionData> latestCorrectInteraction() {
        return interactions.stream()
                .filter(QuestionInteractionData::isCorrect)
                .filter(QuestionInteractionData::allowsMoreSteps)
                .reduce((first, second) -> second);
    }

    public int correctInteractionsCount() {
        return (int) interactions.stream().filter(QuestionInteractionData::isCorrect).count();
    }

    public int erroneousInteractionsCount() {
        return interactions.size() - correctInteractionsCount();
    }
}
