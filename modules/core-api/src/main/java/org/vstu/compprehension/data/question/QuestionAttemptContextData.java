package org.vstu.compprehension.data.question;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.enums.Language;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class QuestionAttemptContextData {
    private long attemptId;
    private Language userLanguage;
    private String strategyId;
    private @NotNull List<ExerciseStageData> stages = new ArrayList<>(0);
    private @NotNull ExerciseStageData questionStage;
    private boolean preferDecisionTreeSupplementary;
}
