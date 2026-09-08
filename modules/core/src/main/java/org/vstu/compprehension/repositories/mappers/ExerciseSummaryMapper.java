package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exercise.ExerciseSummaryData;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.Strict;

/** Упражнение в объёме списка. */
@Component
class ExerciseSummaryMapper implements Mapper<ExerciseEntity, ExerciseSummaryData> {

    @Override
    public @NotNull ExerciseSummaryData map(@NotNull ExerciseEntity source) {
        long id = Strict.required(source.getId(), "id", "exercise");
        return new ExerciseSummaryData(
                id,
                Strict.required(source.getName(), "name", "exercise " + id),
                source.isPublic());
    }
}
