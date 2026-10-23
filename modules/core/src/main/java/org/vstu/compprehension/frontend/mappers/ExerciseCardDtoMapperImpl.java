package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.data.permission.ExerciseCardPermissionsData;
import org.vstu.compprehension.frontend.dto.ExerciseCardDto;
import org.vstu.compprehension.frontend.dto.ExerciseCardPermissionsDto;
import org.vstu.compprehension.frontend.dto.ExerciseStageDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
@RequiredArgsConstructor
class ExerciseCardDtoMapperImpl implements ExerciseCardDtoMapper {
    @Override
    public @NotNull ExerciseCardDto map(@NotNull ExerciseData exercise,
                                        @NotNull ExerciseCardPermissionsData permissions) {
        return ExerciseCardDto.builder()
                .id(exercise.id())
                .name(exercise.name())
                .domainId(exercise.domainId())
                .strategyId(exercise.strategyId())
                .backendId(exercise.backendId())
                .stages(exercise.stages().stream().map(this::map).toList())
                .options(exercise.options())
                .tags(exercise.tags())
                .isPublic(exercise.isPublic())
                .permissions(map(permissions))
                .build();
    }
    
    private @NotNull ExerciseStageDto map(@NotNull ExerciseStageData source) {
        return ExerciseStageDto.builder()
                .numberOfQuestions(source.getNumberOfQuestions())
                .complexity(source.getComplexity())
                .laws(source.getLaws())
                .concepts(source.getConcepts())
                .skills(source.getSkills())
                .build();
    }


    private @NotNull ExerciseCardPermissionsDto map(@NotNull ExerciseCardPermissionsData source) {
        return new ExerciseCardPermissionsDto(
                source.canEdit(),
                source.canDelete(),
                source.canCloneToCourse(),
                source.canCopyToGlobalPool(),
                source.canUnlinkFromCourse());
    }
}
