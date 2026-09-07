package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.frontend.dto.*;
import org.vstu.compprehension.services.ExercisePermissionDataService;
import org.vstu.compprehension.services.ExerciseDataService;

import java.util.List;

@Component
public class ExerciseFrontendServiceImpl implements ExerciseFrontendService {
    private final ExerciseDataService exerciseService;
    private final ExercisePermissionDataService exercisePermissionService;

    public ExerciseFrontendServiceImpl(ExerciseDataService exerciseService, ExercisePermissionDataService exercisePermissionService) {
        this.exerciseService = exerciseService;
        this.exercisePermissionService = exercisePermissionService;
    }

    @Override
    public void ensureCanViewExercise(long userId, long exerciseId) {
        exercisePermissionService.ensureCanViewExercise(userId, exerciseId);
    }

    @Override
    public @NotNull ExerciseInfoDto getExerciseShortInfo(long id, @Nullable Long courseId) {
        return new ExerciseInfoDto(id, exerciseService.getExerciseOptionsInContext(id, courseId));
    }

    @Override
    public void ensureExerciseExists(long exerciseId, @Nullable Long courseId) {
        exerciseService.getExerciseInContext(exerciseId, courseId);
    }

    @Override
    public @NotNull ExerciseCardDto getExerciseCard(long exerciseId, @Nullable Long courseId, long userId) {
        var exercise = exerciseService.getExerciseInContext(exerciseId, courseId);
        return getExerciseCard(exercise, exercisePermissionService.ofExercise(userId, exercise, courseId));
    }

    @Override
    public @NotNull ExerciseListDto listExercises(@Nullable Long courseId, long userId) {
        List<ExerciseDto> exercises = courseId != null
                ? exerciseService.getCourseExercises(courseId)
                : exerciseService.getPublicExercises();
        return new ExerciseListDto(exercises, exercisePermissionService.ofExerciseList(userId, courseId));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void saveExerciseCard(@NotNull ExerciseCardDto card, @Nullable Long courseId) {
        exerciseService.ensureNotInheritedInCourse(card.getId(), courseId);
        exerciseService.saveExerciseCard(card);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public long createExerciseAndGetId(@NotNull String name, @NotNull String domainId, @NotNull String strategyId, @Nullable Long courseId) {
        return exerciseService.createExerciseAndGetId(name, domainId, strategyId, courseId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public long cloneExerciseAndGetId(long sourceExerciseId, @Nullable Long targetCourseId) {
        return exerciseService.cloneExerciseAndGetId(sourceExerciseId, targetCourseId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteExercise(long exerciseId, @Nullable Long courseId) {
        exerciseService.ensureNotInheritedInCourse(exerciseId, courseId);
        exerciseService.deleteExercise(exerciseId);
    }

    private @NotNull ExerciseCardDto getExerciseCard(@NotNull ExerciseData exercise, @NotNull ExerciseCardPermissionsDto permissions) {
        return ExerciseCardDto.builder()
                .id(exercise.id())
                .name(exercise.name())
                .domainId(exercise.domainId())
                .strategyId(exercise.strategyId())
                .backendId(exercise.backendId())
                .stages(exercise.stages().stream()
                        .map(s -> new ExerciseStageDto(s.getNumberOfQuestions(), s.getComplexity(),
                                s.getLaws(), s.getConcepts(), s.getSkills()))
                        .toList())
                .options(exercise.options())
                .tags(exercise.tags())
                .isPublic(exercise.isPublic())
                .permissions(permissions)
                .build();
    }
}
