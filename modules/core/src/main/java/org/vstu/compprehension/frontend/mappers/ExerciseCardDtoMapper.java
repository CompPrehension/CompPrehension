package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.data.permission.ExerciseCardPermissionsData;
import org.vstu.compprehension.frontend.dto.ExerciseCardDto;
import org.vstu.compprehension.mappers.Mapping;

public interface ExerciseCardDtoMapper extends Mapping {

    @NotNull ExerciseCardDto map(@NotNull ExerciseData exercise,
                                 @NotNull ExerciseCardPermissionsData permissions);
}
