package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.AttemptSummaryRow;

import java.util.List;

@Component
class AttemptSummaryMapperImpl implements AttemptSummaryMapper {

    @Override
    public @NotNull AttemptSummaryData map(@NotNull AttemptSummaryRow row,
                                           @NotNull List<Long> questionIds) {
        return new AttemptSummaryData(
                row.getAttemptId(),
                row.getUserId(),
                row.getExerciseId(),
                row.getCourseId(),
                row.getStatus(),
                questionIds);
    }
}
