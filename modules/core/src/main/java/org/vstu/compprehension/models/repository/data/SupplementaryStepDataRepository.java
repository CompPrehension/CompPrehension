package org.vstu.compprehension.models.repository.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.data.NewSupplementaryStepData;
import org.vstu.compprehension.models.data.SupplementaryStepData;
import org.vstu.compprehension.models.entities.SupplementaryStepEntity;
import org.vstu.compprehension.models.repository.InteractionRepository;
import org.vstu.compprehension.models.repository.QuestionRepository;
import org.vstu.compprehension.models.repository.SupplementaryStepRepository;

import java.util.NoSuchElementException;

/**
 * Цепочки вспомогательных вопросов.
 * <p>
 * Один запрос на чтение: шаг — это состояние автомата плюс json с ситуацией, связи из
 * него не поднимаются вовсе. Взаимодействие с главным вопросом отдаётся идентификатором;
 * собрать его целиком умеет {@link QuestionDataRepository#findInteractionById}.
 */
@Repository
@RequiredArgsConstructor
public class SupplementaryStepDataRepository {

    private final SupplementaryStepRepository supplementaryStepRepository;
    private final InteractionRepository interactionRepository;
    private final QuestionRepository questionRepository;

    /** Шаг, породивший этот вспомогательный вопрос; null, если вопрос не из цепочки. */
    @Transactional(readOnly = true)
    public @Nullable SupplementaryStepData findBySupplementaryQuestionId(long supplementaryQuestionId) {
        return toData(supplementaryStepRepository.findRowBySupplementaryQuestion(supplementaryQuestionId));
    }

    /** Последний шаг цепочки, начатой этим взаимодействием; null, если цепочка не начата. */
    @Transactional(readOnly = true)
    public @Nullable SupplementaryStepData findLatestStepOfInteraction(long interactionId) {
        var rows = supplementaryStepRepository.findRowsByMainQuestionInteractionIdOrderByIdDesc(interactionId);
        return rows.isEmpty() ? null : toData(rows.get(0));
    }

    /**
     * Записать новый шаг цепочки.
     *
     * @param supplementaryQuestionId вопрос, сгенерированный на этом шаге; null, если шаг
     *                                лишь фиксирует переход автомата и вопроса не породил
     * @return идентификатор записанного шага
     */
    @Transactional
    public long create(@NotNull NewSupplementaryStepData step, @Nullable Long supplementaryQuestionId) {
        var interaction = interactionRepository.findById(step.getMainQuestionInteractionId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Interaction " + step.getMainQuestionInteractionId() + " not found"));

        var entity = new SupplementaryStepEntity();
        entity.setMainQuestionInteraction(interaction);
        entity.setSituationInfo(step.getSituationInfo());
        entity.setNextStateId(step.getNextStateId());
        if (supplementaryQuestionId != null) {
            entity.setSupplementaryQuestion(questionRepository.getReferenceById(supplementaryQuestionId));
        }
        return supplementaryStepRepository.save(entity).getId();
    }

    private static @Nullable SupplementaryStepData toData(
            @Nullable SupplementaryStepRepository.StepRow row) {
        if (row == null) {
            return null;
        }
        var entity = row.getStep();
        return SupplementaryStepData.builder()
                .id(entity.getId())
                .mainQuestionInteractionId(row.getMainQuestionInteractionId())
                .situationInfo(entity.getSituationInfo())
                .nextStateId(entity.getNextStateId())
                .build();
    }
}
