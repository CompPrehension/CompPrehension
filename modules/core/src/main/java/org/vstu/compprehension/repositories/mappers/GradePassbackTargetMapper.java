package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exerciseattempt.GradePassbackTargetData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.GradePassbackTargetRow;

@Component
class GradePassbackTargetMapper implements Mapper<GradePassbackTargetRow, GradePassbackTargetData> {

    @Override
    public @NotNull GradePassbackTargetData map(@NotNull GradePassbackTargetRow source) {
        return new GradePassbackTargetData(
                source.getAttemptId(),
                source.getExerciseId(),
                source.getUserId(),
                source.getExternalUserId(),
                source.getLtiLineitemUrl(),
                source.getLtiIssuer(),
                source.getLtiClientId(),
                source.getCourseId());
    }
}
