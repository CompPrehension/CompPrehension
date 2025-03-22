package org.vstu.compprehension.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class FeedbackViolationLawDto {
    private String name;
    private boolean canCreateSupplementaryQuestion;
}
