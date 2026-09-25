package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.InteractionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseData {
    private Long id;
    private @NotNull AnswerData answer;
    private @Nullable InteractionType createdByInteractionType;
    private @Nullable Long createdByInteractionId;

    /**
     * Были ли нарушения во взаимодействии, которому принадлежит ответ.
     */
    private boolean interactionHasViolations;
}
