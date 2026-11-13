package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.businesslogic.strategies.StrategyDecision;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptStageData;
import org.vstu.compprehension.enums.Decision;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
class AttemptStageServiceImpl implements AttemptStageService {
    private final ExerciseAttemptDataService exerciseAttemptService;
    private final AbstractStrategyFactory strategyFactory;

    // Когда пройдены все этапы, вопросы, которые студент запрашивает сверх плана, задаются для последнего.
    public @NotNull AttemptStageData findCurrentStage(long attemptId) {
        var stages = toStages(exerciseAttemptService.getAttemptWithQuestions(attemptId));
        return stages.stream()
                .filter(stage -> !isPassed(stage))
                .findFirst()
                .orElse(stages.getLast());
    }

    // Оценка этапа весит столько, сколько вопросов на него запланировано; этап, где вопросов ещё не было, оценён нулём.
    public @NotNull StrategyDecision gradeAndDecide(long attemptId) {
        float weightedGrade = 0;
        int plannedQuestions = 0;
        boolean isFinished = true;
        for (var stage : toStages(exerciseAttemptService.getAttemptWithQuestions(attemptId))) {
            int planned = stage.getStage().getNumberOfQuestions();
            plannedQuestions += planned;
            if (!stage.getQuestions().isEmpty()) {
                weightedGrade += findStrategy(stage).grade(stage) * planned;
            }
            isFinished = isFinished && isPassed(stage);
        }
        return new StrategyDecision(weightedGrade / plannedQuestions, isFinished ? Decision.FINISH : Decision.CONTINUE);
    }

    // Этап заканчивается только на завершённом вопросе: когда заданы все запланированные вопросы
    // или стратегия решила, что этап пройден раньше.
    private boolean isPassed(@NotNull AttemptStageData stage) {
        var questions = stage.getQuestions();
        if (questions.isEmpty() || !questions.getLast().isCompleted()) {
            return false;
        }
        long completed = questions.stream().filter(AttemptQuestionData::isCompleted).count();
        return completed >= stage.getStage().getNumberOfQuestions()
                || findStrategy(stage).decide(stage) == Decision.FINISH;
    }

    private @NotNull AbstractStrategy findStrategy(@NotNull AttemptStageData stage) {
        return strategyFactory.getStrategy(stage.getStage().getStrategyId());
    }

    private static @NotNull List<AttemptStageData> toStages(@NotNull ExerciseAttemptWithQuestionsData attempt) {
        int stageCount = attempt.exercise().stages().size();
        if (stageCount == 0) {
            throw new IllegalStateException("Exercise of attempt " + attempt.id() + " has no stages");
        }
        return IntStream.range(0, stageCount)
                .mapToObj(index -> new AttemptStageData(attempt, index))
                .toList();
    }
}
