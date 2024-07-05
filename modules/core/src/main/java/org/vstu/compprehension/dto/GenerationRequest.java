package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;

@AllArgsConstructor
@Value
public class GenerationRequest {
    Integer[] generationRequestIds;
    QuestionBankSearchRequest questionRequest;
    int questionsToGenerate;
    int questionsGenerated;
}
