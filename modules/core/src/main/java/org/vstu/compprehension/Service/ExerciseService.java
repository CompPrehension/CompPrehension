package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseCardPermissionsDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.dto.ExerciseStageDto;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.data.ExerciseCardUpdateData;
import org.vstu.compprehension.models.data.ExerciseData;
import org.vstu.compprehension.models.data.ExerciseOptionsData;
import org.vstu.compprehension.models.data.ExerciseStageData;
import org.vstu.compprehension.models.data.ExerciseSummaryData;
import org.vstu.compprehension.models.data.NewExerciseData;
import org.vstu.compprehension.models.repository.data.ExerciseDataRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ExerciseService {

    private final ExerciseDataRepository exercises;
    private final DomainFactory domainFactory;
    private final CourseService courseService;

    /** Публично ли упражнение. */
    @Transactional(readOnly = true)
    public boolean isExercisePublic(long exerciseId) {
        return exercises.getById(exerciseId).isPublic();
    }

    /** Упражнение по идентификатору. */
    @Transactional(readOnly = true)
    public @NotNull ExerciseData getExercise(long exerciseId) {
        return exercises.getById(exerciseId);
    }

    /** Настройки упражнения в контексте курса. */
    @Transactional(readOnly = true)
    public @NotNull ExerciseOptionsData getExerciseOptionsInContext(long exerciseId, @Nullable Long courseId) {
        return getExerciseInContext(exerciseId, courseId).options();
    }

    /**
     * Упражнение, доступное из этого контекста.
     * <p>
     * Вне курса видно только глобальный пул; из курса — то, что в нём показано.
     *
     * @throws IllegalStateException если упражнение не относится к этому контексту
     */
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

    /**
     * Упражнение показано в курсе, но принадлежит глобальному пулу. Из курса оно доступно
     * только на чтение: правка или удаление затронули бы все курсы, которые его наследуют.
     */
    public static boolean isInheritedInCourse(@NotNull ExerciseData exercise, @Nullable Long courseId) {
        return courseId != null && exercise.isPublic();
    }

    /**
     * Упражнение показано в курсе, но принадлежит глобальному пулу — править его нельзя.
     *
     * @throws IllegalStateException если упражнение унаследовано в этот курс
     */
    @Transactional(readOnly = true)
    public void ensureNotInheritedInCourse(long exerciseId, @Nullable Long courseId) {
        if (isInheritedInCourse(getExerciseInContext(exerciseId, courseId), courseId)) {
            throw new IllegalStateException("inherited_exercise_is_read_only");
        }
    }

    /**
     * Завести упражнение с настройками по умолчанию.
     * <p>
     * Заведённое вне курса попадает в глобальный пул, заведённое в курсе принадлежит
     * этому курсу.
     *
     * @return идентификатор созданного упражнения
     */
    @Transactional
    public long createExerciseAndGetId(@NotNull String name, @NotNull String domainId,
                                       @NotNull String strategyId, @Nullable Long courseId) {
        // Решатель определяется предметной областью, а не выбором преподавателя.
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

    /**
     * Скопировать упражнение.
     * <p>
     * Копирование между курсами запрещено: приватное упражнение живёт в своём курсе,
     * и чтобы отдать его другому, его сначала переносят в глобальный пул.
     *
     * @return идентификатор копии
     * @throws IllegalStateException если копируется приватное упражнение в курс
     */
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
        // Решатель определяется предметной областью, а не карточкой: пришедшее с фронта
        // значение backendId игнорируется.
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

    public @NotNull ExerciseCardDto getExerciseCard(@NotNull ExerciseData exercise,
                                                    @NotNull ExerciseCardPermissionsDto permissions) {
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

    private static @NotNull List<ExerciseDto> toExerciseDtos(
            @NotNull List<ExerciseSummaryData> summaries) {
        return summaries.stream()
                .map(e -> new ExerciseDto(e.id(), e.name(), e.isPublic()))
                .toList();
    }
}
