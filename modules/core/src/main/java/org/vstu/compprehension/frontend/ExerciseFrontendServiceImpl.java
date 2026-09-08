package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.frontend.mappers.ExerciseCardDtoMapper;
import org.vstu.compprehension.frontend.mappers.ExerciseInfoDtoMapper;
import org.vstu.compprehension.frontend.mappers.ExerciseListDtoMapper;
import org.vstu.compprehension.frontend.dto.*;
import org.vstu.compprehension.services.ExercisePermissionDataService;
import org.vstu.compprehension.services.ExerciseDataService;

import java.util.List;

@Component
public class ExerciseFrontendServiceImpl implements ExerciseFrontendService {
    private final ExerciseDataService exerciseService;
    private final ExercisePermissionDataService exercisePermissionService;
    private final ExerciseCardDtoMapper exerciseCardDtoMapper;
    private final ExerciseListDtoMapper exerciseListDtoMapper;
    private final ExerciseInfoDtoMapper exerciseInfoDtoMapper;

    public ExerciseFrontendServiceImpl(ExerciseDataService exerciseService,
                                       ExercisePermissionDataService exercisePermissionService,
                                       ExerciseCardDtoMapper exerciseCardDtoMapper,
                                       ExerciseListDtoMapper exerciseListDtoMapper,
                                       ExerciseInfoDtoMapper exerciseInfoDtoMapper) {
        this.exerciseService = exerciseService;
        this.exercisePermissionService = exercisePermissionService;
        this.exerciseCardDtoMapper = exerciseCardDtoMapper;
        this.exerciseListDtoMapper = exerciseListDtoMapper;
        this.exerciseInfoDtoMapper = exerciseInfoDtoMapper;
    }

    @Override
    public void ensureCanViewExercise(long userId, long exerciseId) {
        exercisePermissionService.ensureCanViewExercise(userId, exerciseId);
    }

    @Override
    public @NotNull ExerciseInfoDto getExerciseShortInfo(long id, @Nullable Long courseId) {
        return exerciseInfoDtoMapper.map(id, exerciseService.getExerciseOptionsInContext(id, courseId));
    }

    @Override
    public void ensureExerciseExists(long exerciseId, @Nullable Long courseId) {
        exerciseService.getExerciseInContext(exerciseId, courseId);
    }

    @Override
    public @NotNull ExerciseCardDto getExerciseCard(long exerciseId, @Nullable Long courseId, long userId) {
        var exercise = exerciseService.getExerciseInContext(exerciseId, courseId);
        return exerciseCardDtoMapper.map(
                exercise, exercisePermissionService.ofExercise(userId, exercise, courseId));
    }

    @Override
    public @NotNull ExerciseListDto listExercises(@Nullable Long courseId, long userId) {
        var exercises = courseId != null
                ? exerciseService.getCourseExercises(courseId)
                : exerciseService.getPublicExercises();
        return exerciseListDtoMapper.map(
                exercises, exercisePermissionService.ofExerciseList(userId, courseId));
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
}
