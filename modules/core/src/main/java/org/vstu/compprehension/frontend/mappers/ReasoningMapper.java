package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.domains.Reasoning;
import org.vstu.compprehension.data.question.InteractionReasoningData;
import org.vstu.compprehension.mappers.UpdateMapper;

@Component
class ReasoningMapper implements UpdateMapper<Reasoning, InteractionReasoningData> {

    @Override
    public void apply(@NotNull Reasoning source, @NotNull InteractionReasoningData destination) {
        destination.setId(source.id());
        destination.setAssumptions(source.assumptions());
        destination.setCorrect(source.isCorrect());
        destination.setReason(source.reason());
        destination.setViolations(source.violations());
        destination.setAppliedLaws(source.appliedLaws());
    }
}
