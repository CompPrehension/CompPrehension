package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.domains.Judgement;
import org.vstu.compprehension.data.question.InteractionReasoningData;
import org.vstu.compprehension.mappers.UpdateMapper;

@Component
class VerdictMapper implements UpdateMapper<Judgement.Verdict, InteractionReasoningData> {

    @Override
    public void apply(@NotNull Judgement.Verdict source, @NotNull InteractionReasoningData destination) {
        destination.setCorrect(source.isAnswerCorrect());
        destination.setViolations(source.violations());
        destination.setAppliedLaws(source.appliedLaws());
    }
}
