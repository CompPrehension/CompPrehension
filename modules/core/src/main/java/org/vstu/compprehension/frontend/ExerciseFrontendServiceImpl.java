package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.frontend.mappers.ExerciseCardDtoMapper;
import org.vstu.compprehension.frontend.mappers.ExerciseListDtoMapper;
import org.vstu.compprehension.frontend.dto.*;
import org.vstu.compprehension.mappers.UpdateMapper;
import org.vstu.compprehension.services.ExerciseDataService;

@Component
public class ExerciseFrontendServiceImpl implements ExerciseFrontendService {
    private final ExerciseDataService exerciseService;
    private final AuthFrontendService authService;
    private final ExerciseCardDtoMapper exerciseCardDtoMapper;
    private final ExerciseListDtoMapper exerciseListDtoMapper;
    private final UpdateMapper<ExerciseOptionsData, ExerciseInfoDto> exerciseInfoDtoMapper;

    public ExerciseFrontendServiceImpl(ExerciseDataService exerciseService,
                                       AuthFrontendService authService,
                                       ExerciseCardDtoMapper exerciseCardDtoMapper,
                                       ExerciseListDtoMapper exerciseListDtoMapper,
                                       UpdateMapper<ExerciseOptionsData, ExerciseInfoDto> exerciseInfoDtoMapper) {
        this.exerciseService = exerciseService;
        this.authService = authService;
        this.exerciseCardDtoMapper = exerciseCardDtoMapper;
        this.exerciseListDtoMapper = exerciseListDtoMapper;
        this.exerciseInfoDtoMapper = exerciseInfoDtoMapper;
    }

    @Override
    public @NotNull ExerciseInfoDto getExerciseShortInfo(long id, @Nullable Long courseId) {

        var exerciseOptions = exerciseService.getExercise(id).options();
        
        var result = new ExerciseInfoDto();
        result.setId(id);
        exerciseInfoDtoMapper.apply(exerciseOptions, result);

        return result;
    }

    @Override
    public boolean isExercisePublic(long exerciseId) {
        return exerciseService.isExercisePublic(exerciseId);
    }

    @Override
    public @NotNull ExerciseCardDto getExerciseCard(long exerciseId, @Nullable Long courseId, long userId) {
        var exercise = exerciseService.getExercise(exerciseId);
        return exerciseCardDtoMapper.map(
                exercise, authService.getExerciseCardPermissions(userId, exercise, courseId));
    }

    @Override
    public @NotNull ExerciseListDto listExercises(@Nullable Long courseId, long userId) {
        var exercises = courseId != null
                ? exerciseService.getCourseExercises(courseId)
                : exerciseService.getPublicExercises();
        return exerciseListDtoMapper.map(
                exercises, authService.getExerciseListPermissions(userId, courseId));
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
