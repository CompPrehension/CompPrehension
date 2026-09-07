package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.SpecValue;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseData {
    private Long id;
    private SpecValue specValue;
    private AnswerObjectData leftAnswerObject;
    private AnswerObjectData rightAnswerObject;
    private @Nullable InteractionType createdByInteractionType;
    private @Nullable Long createdByInteractionId;

    /**
     * Были ли нарушения во взаимодействии, которому принадлежит ответ.
     * <p>
     * Проекция вместо обратной ссылки на взаимодействие: единственное, что из него
     * читалось — {@code response.getInteraction().getViolations().isEmpty()}.
     */
    private boolean interactionHasViolations;
}
