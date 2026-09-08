package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exercise.ExerciseSummaryData;
import org.vstu.compprehension.data.permission.ExerciseListPermissionsData;
import org.vstu.compprehension.frontend.dto.ExerciseDto;
import org.vstu.compprehension.frontend.dto.ExerciseListDto;
import org.vstu.compprehension.frontend.dto.ExerciseListPermissionsDto;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;

@Component
@RequiredArgsConstructor
class ExerciseListDtoMapperImpl implements ExerciseListDtoMapper {
    @Override
    public @NotNull ExerciseListDto map(@NotNull List<ExerciseSummaryData> exercises,
                                        @NotNull ExerciseListPermissionsData permissions) {
        return new ExerciseListDto(
                exercises.stream().map(this::map).toList(),
                map(permissions));
    }

    private @NotNull ExerciseDto map(@NotNull ExerciseSummaryData source) {
        return new ExerciseDto(source.id(), source.name(), source.isPublic());
    }

    private @NotNull ExerciseListPermissionsDto map(@NotNull ExerciseListPermissionsData source) {
        return new ExerciseListPermissionsDto(
                source.canCreateExercise(),
                source.canImportInherit(),
                source.canImportClone());
    }
}
