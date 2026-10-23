package org.vstu.compprehension.data.questionbank;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.vstu.compprehension.businesslogic.QuestionBankSearchRequest;

@AllArgsConstructor
@Value
public class GenerationRequestGroupData {
    Integer[] generationRequestIds;
    GenerationRequestData[] generationRequests;
    QuestionBankSearchRequest questionRequest;
    int questionsToGenerate;
}
