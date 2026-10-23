package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.frontend.dto.ExerciseAttemptDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class ExerciseAttemptDtoMapper implements Mapper<AttemptSummaryData, ExerciseAttemptDto> {

    @Override
    public @NotNull ExerciseAttemptDto map(@NotNull AttemptSummaryData source) {
        return ExerciseAttemptDto.builder()
                .userId(source.userId())
                .exerciseId(source.exerciseId())
                .courseId(source.courseId())
                .attemptId(source.attemptId())
                .questionIds(source.questionIds().toArray(Long[]::new))
                .status(source.status())
                .build();
    }
}
