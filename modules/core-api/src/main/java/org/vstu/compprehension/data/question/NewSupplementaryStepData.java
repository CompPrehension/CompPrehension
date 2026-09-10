package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewSupplementaryStepData {
    private long mainQuestionInteractionId;
    private SupplementarySituationData situationInfo;
    private @Nullable Integer nextStateId;
}
