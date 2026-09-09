package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.enums.SupplementaryBranchResult;

import java.util.Map;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupplementarySituationData {
    private Map<String, String> reasoningVariables;
    private Map<String, String> discussedVariables;
    private Map<Integer, Integer> givenAnswers;
    private Map<String, SupplementaryBranchResult> assumedResults;
    private String localizationCode;
}
