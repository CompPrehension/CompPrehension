package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.SpecValue;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseData implements AnswerData {
    private Long id;
    private SpecValue specValue;
    private @NotNull AnswerObjectData leftAnswerObject;
    private @NotNull AnswerObjectData rightAnswerObject;
    private @Nullable InteractionType createdByInteractionType;
    private @Nullable Long createdByInteractionId;

    /**
     * Были ли нарушения во взаимодействии, которому принадлежит ответ.
     */
    private boolean interactionHasViolations;
}
