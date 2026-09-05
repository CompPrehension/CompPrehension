package org.vstu.compprehension.models.data;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;

@AllArgsConstructor
@Value
public class GenerationRequestGroupData {
    Integer[] generationRequestIds;
    GenerationRequestData[] generationRequests;
    QuestionBankSearchRequest questionRequest;
    int questionsToGenerate;
}
