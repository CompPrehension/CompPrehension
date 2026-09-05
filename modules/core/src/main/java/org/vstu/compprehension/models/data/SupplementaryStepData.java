package org.vstu.compprehension.models.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * Шаг цепочки вспомогательных вопросов в объёме, нужном доменам.
 * <p>
 * Домен читает отсюда состояние автомата и ситуацию, в которой был задан предыдущий
 * вопрос. Ссылки на сущности вопроса и попытки здесь нет: их проставляет сервис при
 * сохранении.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplementaryStepData {
    private Long id;
    private @Nullable QuestionInteractionData mainQuestionInteraction;
    private SupplementarySituationData situationInfo;
    private Integer nextStateId;
}
