package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseSummaryData;
import org.vstu.compprehension.data.permission.ExerciseListPermissionsData;
import org.vstu.compprehension.frontend.dto.ExerciseListDto;
import org.vstu.compprehension.mappers.Mapping;

import java.util.List;

public interface ExerciseListDtoMapper extends Mapping {

    @NotNull ExerciseListDto map(@NotNull List<ExerciseSummaryData> exercises,
                                 @NotNull ExerciseListPermissionsData permissions);
}
