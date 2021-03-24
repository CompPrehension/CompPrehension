package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder @Jacksonized
public class InteractionDto {
    private Long attemptId;
    private Long questionId;
    private Long[][] answers;
}
