package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.question.NewSupplementaryStepData;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.entities.SupplementaryStepEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.InteractionRepository;
import org.vstu.compprehension.repositories.entity.QuestionRepository;
import org.vstu.compprehension.repositories.entity.SupplementaryStepRepository.StepRow;
import org.vstu.compprehension.repositories.entity.SupplementaryStepRepository;

import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SupplementaryStepDataRepository {

    private final SupplementaryStepRepository supplementaryStepRepository;
    private final InteractionRepository interactionRepository;
    private final QuestionRepository questionRepository;
    private final Mapper<StepRow, SupplementaryStepData> supplementaryStepMapper;

    /** Шаг, породивший этот вспомогательный вопрос; null, если вопрос не из цепочки. */
    @Transactional(readOnly = true)
    public @Nullable SupplementaryStepData findBySupplementaryQuestionId(long supplementaryQuestionId) {
        return Optional.ofNullable(
                        supplementaryStepRepository.findRowBySupplementaryQuestion(supplementaryQuestionId))
                .map(supplementaryStepMapper::map)
                .orElse(null);
    }

    /** Последний шаг цепочки, начатой этим взаимодействием; null, если цепочка не начата. */
    @Transactional(readOnly = true)
    public @Nullable SupplementaryStepData findLatestStepOfInteraction(long interactionId) {
        var rows = supplementaryStepRepository.findRowsByMainQuestionInteractionIdOrderByIdDesc(interactionId);
        return rows.isEmpty() ? null : supplementaryStepMapper.map(rows.get(0));
    }

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
}
