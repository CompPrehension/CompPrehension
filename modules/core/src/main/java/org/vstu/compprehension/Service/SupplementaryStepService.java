package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.SupplementaryStepEntity;
import org.vstu.compprehension.models.repository.InteractionRepository;
import org.vstu.compprehension.models.repository.SupplementaryStepRepository;

import java.util.List;

/**
 * Цепочки вспомогательных вопросов.
 * <p>
 * Заведён потому, что доменам нужны сущности взаимодействия и шага цепочки — это записи
 * в БД, а вопрос в бизнес-логике их больше не держит. Домены обращаются сюда по
 * идентификатору вместо обхода ленивых связей.
 */
@Service
@RequiredArgsConstructor
public class SupplementaryStepService {

    private final SupplementaryStepRepository supplementaryStepRepository;
    private final InteractionRepository interactionRepository;

    /** Последний шаг цепочки для взаимодействия; пусто, если цепочка ещё не начата. */
    @Transactional(readOnly = true)
    public @Nullable SupplementaryStepEntity findLatestStepOfInteraction(long interactionId) {
        List<SupplementaryStepEntity> steps =
                supplementaryStepRepository.findAllByMainQuestionInteractionIdOrderByIdAsc(interactionId);
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }

    /** Взаимодействие по идентификатору — нужно, чтобы связать с ним новый шаг цепочки. */
    @Transactional(readOnly = true)
    public @NotNull InteractionEntity getInteraction(long interactionId) {
        return interactionRepository.findById(interactionId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Interaction " + interactionId + " not found"));
    }
}
