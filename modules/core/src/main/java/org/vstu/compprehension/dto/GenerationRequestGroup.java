package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;

@AllArgsConstructor
@Value
public class GenerationRequestGroup {
    Integer[] generationRequestIds;
    GenerationRequest[] generationRequests;
    QuestionBankSearchRequest questionRequest;
    int questionsToGenerate;
}
