package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.repositories.data.QuestionDataRepository;
import org.vstu.compprehension.repositories.data.SupplementaryStepDataRepository;

@Service
@RequiredArgsConstructor
class SupplementaryStepDataServiceImpl implements SupplementaryStepDataService {

    private final SupplementaryStepDataRepository supplementaryStepDataRepository;
    private final QuestionDataRepository questionDataRepository;

    @Transactional(readOnly = true)
    public @Nullable SupplementaryStepData findLatestStepOfInteraction(long interactionId) {
        return supplementaryStepDataRepository.findLatestStepOfInteraction(interactionId);
    }

    @Transactional(readOnly = true)
    public @NotNull QuestionData getMainQuestionOfInteraction(long interactionId) {
        return questionDataRepository.findByInteractionId(interactionId);
    }
}
