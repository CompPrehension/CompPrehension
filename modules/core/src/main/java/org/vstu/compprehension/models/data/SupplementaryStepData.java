package org.vstu.compprehension.models.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * Шаг цепочки вспомогательных вопросов: состояние автомата и ситуация, в которой был
 * задан предыдущий вопрос.
 * <p>
 * Взаимодействие с главным вопросом здесь только идентификатором. Раньше тут лежал
 * {@link QuestionInteractionData}, и это была ошибка: собрать его вместе с обратной
 * ссылкой на вопрос из выборки шага невозможно, поэтому ссылка оставалась пустой,
 * а домен на ней падал. Кому нужно само взаимодействие — берёт его по идентификатору
 * у {@code SupplementaryStepService}, где оно собирается целиком.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplementaryStepData {
    private Long id;
    private long mainQuestionInteractionId;
    private SupplementarySituationData situationInfo;
    private @Nullable Integer nextStateId;
}
