package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.data.QuestionInteractionData;
import org.vstu.compprehension.models.data.SupplementaryStepData;
import org.vstu.compprehension.models.repository.data.QuestionDataRepository;
import org.vstu.compprehension.models.repository.data.SupplementaryStepDataRepository;

/**
 * Цепочки вспомогательных вопросов — то, что о них нужно знать доменам.
 * <p>
 * Домены обращаются сюда по идентификатору: ни сущностей, ни ленивых связей у них нет.
 * Сервис не собирает данные сам, а берёт их у {@code models.repository.data}, где форма
 * выборки и маппинг заданы вместе.
 */
@Service
@RequiredArgsConstructor
public class SupplementaryStepService {

    private final SupplementaryStepDataRepository supplementaryStepDataRepository;
    private final QuestionDataRepository questionDataRepository;

    /** Последний шаг цепочки для взаимодействия; null, если цепочка ещё не начата. */
    @Transactional(readOnly = true)
    public @Nullable SupplementaryStepData findLatestStepOfInteraction(long interactionId) {
        return supplementaryStepDataRepository.findLatestStepOfInteraction(interactionId);
    }

    /**
     * Взаимодействие с главным вопросом, с которого началась цепочка.
     * <p>
     * Возвращается вместе с вопросом: домен строит по нему модель ситуации.
     */
    @Transactional(readOnly = true)
    public @NotNull QuestionInteractionData getMainQuestionInteraction(long interactionId) {
        return questionDataRepository.findInteractionById(interactionId);
    }
}
