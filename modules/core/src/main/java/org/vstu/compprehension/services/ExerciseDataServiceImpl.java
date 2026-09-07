package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.frontend.dto.ExerciseCardDto;
import org.vstu.compprehension.frontend.dto.ExerciseDto;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.data.exercise.ExerciseCardUpdateData;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.data.exercise.ExerciseSummaryData;
import org.vstu.compprehension.data.exercise.NewExerciseData;
import org.vstu.compprehension.repositories.data.ExerciseDataRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
class ExerciseDataServiceImpl implements ExerciseDataService {

    private final ExerciseDataRepository exercises;
    private final DomainFactory domainFactory;
    private final CourseDataService courseService;

    @Transactional(readOnly = true)
    public boolean isExercisePublic(long exerciseId) {
        return exercises.getById(exerciseId).isPublic();
    }

    @Transactional(readOnly = true)
    public @NotNull ExerciseData getExercise(long exerciseId) {
        return exercises.getById(exerciseId);
    }

    @Transactional(readOnly = true)
    public @NotNull ExerciseOptionsData getExerciseOptionsInContext(long exerciseId, @Nullable Long courseId) {
        return getExerciseInContext(exerciseId, courseId).options();
    }

    @Transactional(readOnly = true)
    public @NotNull ExerciseData getExerciseInContext(long exerciseId, @Nullable Long courseId) {
        var exercise = exercises.getById(exerciseId);
        if (courseId == null) {
            if (!exercise.isPublic()) {
                throw new IllegalStateException("exercise_not_in_global_pool");
            }
        } else {
            courseService.ensureExerciseInCourse(exerciseId, courseId);
        }
        return exercise;
    }

    public boolean isInheritedInCourse(@NotNull ExerciseData exercise, @Nullable Long courseId) {
        return courseId != null && exercise.isPublic();
    }

    @Transactional(readOnly = true)
    public void ensureNotInheritedInCourse(long exerciseId, @Nullable Long courseId) {
        if (isInheritedInCourse(getExerciseInContext(exerciseId, courseId), courseId)) {
            throw new IllegalStateException("inherited_exercise_is_read_only");
        }
    }

    @Transactional
    public long createExerciseAndGetId(@NotNull String name, @NotNull String domainId, @NotNull String strategyId, @Nullable Long courseId) {
        var backendId = domainFactory.getDomain(domainId).getBackendId();

        long exerciseId = exercises.create(new NewExerciseData(
                name,
                domainId,
                backendId,
                strategyId,
                ExerciseOptionsData.builder()
                        .forceNewAttemptCreationEnabled(true)
                        .correctAnswerGenerationEnabled(true)
                        .newQuestionGenerationEnabled(true)
                        .supplementaryQuestionsEnabled(true)
                        .debugButtonEnabled(false)
                        .preferDecisionTreeBasedSupplementaryEnabled(false)
                        .build(),
                List.of(new ExerciseStageData(5, 0.5f, new ArrayList<>(), new ArrayList<>(), new ArrayList<>())),
                List.of(),
                courseId == null));

        if (courseId != null) {
            courseService.linkExerciseWithCourseIfMissing(exerciseId, courseId);
        }
        return exerciseId;
    }

    @Transactional
    public long cloneExerciseAndGetId(long sourceExerciseId, @Nullable Long targetCourseId) {
        var source = exercises.getById(sourceExerciseId);

        if (!source.isPublic() && targetCourseId != null) {
            var sourceCourseIds = courseService.findCourseIdsByExerciseId(sourceExerciseId);
            if (sourceCourseIds.size() == 1 && targetCourseId.equals(sourceCourseIds.get(0))) {
                throw new IllegalStateException("duplicating_in_same_course");
            }
            throw new IllegalStateException("course_to_course_forbidden: copy to pool first, then link");
        }

        long cloneId = exercises.copy(sourceExerciseId, targetCourseId == null);
        if (targetCourseId != null) {
            courseService.linkExerciseWithCourseIfMissing(cloneId, targetCourseId);
        }
        return cloneId;
    }

    @Transactional
    public void deleteExercise(long exerciseId) {
        exercises.delete(exerciseId);
    }

    @Transactional(readOnly = true)
    public @NotNull List<ExerciseDto> getCourseExercises(long courseId) {
        return toExerciseDtos(exercises.findSummariesByCourseId(courseId));
    }

    @Transactional(readOnly = true)
    public @NotNull List<ExerciseDto> getPublicExercises() {
        return toExerciseDtos(exercises.findPublicSummaries());
    }

    @Transactional
    public void saveExerciseCard(@NotNull ExerciseCardDto card) {
        var backendId = domainFactory.getDomain(card.getDomainId()).getBackendId();

        exercises.updateCard(new ExerciseCardUpdateData(
                card.getId(),
                card.getName(),
                card.getDomainId(),
                backendId,
                card.getStrategyId(),
                card.getOptions(),
                card.getStages().stream()
                        .map(s -> new ExerciseStageData(s.getNumberOfQuestions(), s.getComplexity(),
                                s.getLaws(), s.getConcepts(), s.getSkills()))
                        .toList(),
                card.getTags()));
    }

    private static @NotNull List<ExerciseDto> toExerciseDtos(@NotNull List<ExerciseSummaryData> summaries) {
        return summaries.stream()
                .map(e -> new ExerciseDto(e.id(), e.name(), e.isPublic()))
                .toList();
    }
}
