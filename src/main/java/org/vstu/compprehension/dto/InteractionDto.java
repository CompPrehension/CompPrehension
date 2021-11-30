package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder @Jacksonized
@AllArgsConstructor @NoArgsConstructor
public class InteractionDto {
    private Long attemptId;
    private Long questionId;
    private AnswerDto[] answers;
}
