package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionInteractionData {
    private Long id;
    private InteractionType interactionType;
    private @Nullable FeedbackData feedback;
    @Builder.Default
    private List<ViolationData> violations = new ArrayList<>();
    @Builder.Default
    private List<ResponseData> responses = new ArrayList<>();
    @Builder.Default
    private List<CorrectLawData> correctLaw = new ArrayList<>();
}
