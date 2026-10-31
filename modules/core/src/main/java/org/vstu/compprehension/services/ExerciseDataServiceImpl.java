package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.frontend.dto.ExerciseCardDto;
import org.vstu.compprehension.frontend.dto.ExerciseDto;
import org.vstu.compprehension.frontend.dto.ExerciseStageDto;
import org.vstu.compprehension.businesslogic.reaction.AnswerReactionSettings;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.data.exercise.ExerciseCardUpdateData;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.data.exercise.ExerciseSummaryData;
import org.vstu.compprehension.data.exercise.NewExerciseData;
import org.vstu.compprehension.repositories.data.ExerciseDataRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
class ExerciseDataServiceImpl implements ExerciseDataService {

    private final ExerciseDataRepository exercises;
    private final DomainFactory domainFactory;
    private final AbstractStrategyFactory strategyFactory;
    private final CourseDataService courseService;

    @Transactional(readOnly = true)
    public boolean isExercisePublic(long exerciseId) {
        return exercises.getById(exerciseId).isPublic();
    }

    @Transactional(readOnly = true)
    public @NotNull ExerciseData getExercise(long exerciseId) {
        return exercises.getById(exerciseId);
    }

    public boolean isInheritedInCourse(@NotNull ExerciseData exercise, @Nullable Long courseId) {
        return courseId != null && exercise.isPublic();
    }

    @Transactional(readOnly = true)
    public void ensureNotInheritedInCourse(long exerciseId, @Nullable Long courseId) {
        if (isInheritedInCourse(getExercise(exerciseId), courseId)) {
            throw new IllegalStateException("inherited_exercise_is_read_only");
        }
    }

    @Transactional
    public long createExerciseAndGetId(@NotNull String name, @NotNull String domainId, @NotNull String strategyId, @Nullable Long courseId) {
        var backendId = domainFactory.getDomain(domainId).getBackendId();
        var strategySettings = strategyFactory.getStrategy(strategyId).getSettingsType().getDefaults();

        long exerciseId = exercises.create(new NewExerciseData(
                name,
                domainId,
                backendId,
                ExerciseOptionsData.builder()
                        .forceNewAttemptCreationEnabled(true)
                        .correctAnswerGenerationEnabled(true)
                        .newQuestionGenerationEnabled(true)
                        .supplementaryQuestionsEnabled(true)
                        .debugButtonEnabled(false)
                        .build(),
                List.of(ExerciseStageData.builder().strategyId(strategyId).strategySettings(strategySettings).build()),
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
    public @NotNull List<ExerciseSummaryData> getCourseExercises(long courseId) {
        return exercises.findSummariesByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public @NotNull List<ExerciseSummaryData> getPublicExercises() {
        return exercises.findPublicSummaries();
    }

    @Transactional
    public void saveExerciseCard(@NotNull ExerciseCardDto card) {
        var backendId = domainFactory.getDomain(card.getDomainId()).getBackendId();
        var options = card.getOptions().withAnswerReaction(
                AnswerReactionSettings.TYPE.normalize(card.getOptions().getAnswerReaction()));
        if (card.getStages().isEmpty()) {
            throw new IllegalArgumentException("Exercise " + card.getId() + " has no stages");
        }

        exercises.updateCard(new ExerciseCardUpdateData(
                card.getId(),
                card.getName(),
                card.getDomainId(),
                backendId,
                options,
                card.getStages().stream().map(this::toStageData).toList(),
                card.getTags()));
    }

    private @NotNull ExerciseStageData toStageData(@NotNull ExerciseStageDto stage) {
        if (stage.getNumberOfQuestions() < 1) {
            throw new IllegalArgumentException("A stage plans at least one question, got: " + stage.getNumberOfQuestions());
        }
        var settingsType = strategyFactory.getStrategy(stage.getStrategyId()).getSettingsType();
        return new ExerciseStageData(stage.getStrategyId(), settingsType.normalize(stage.getStrategySettings()),
                stage.getNumberOfQuestions(), stage.getComplexity(), stage.getLaws(), stage.getConcepts(), stage.getSkills());
    }
}
