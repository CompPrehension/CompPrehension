package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.exercise.ExerciseCardUpdateData;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.data.exercise.ExerciseSummaryData;
import org.vstu.compprehension.data.exercise.NewExerciseData;
import org.vstu.compprehension.entities.DomainEntity;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.repositories.entity.DomainRepository;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptReassignExecutor;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository;
import org.vstu.compprehension.repositories.entity.ExerciseCourseLinkReassignExecutor;
import org.vstu.compprehension.repositories.entity.ExerciseCourseLinkRepository;
import org.vstu.compprehension.repositories.entity.ExerciseRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

@Repository
@RequiredArgsConstructor
public class ExerciseDataRepository {
    private final ExerciseRepository exerciseRepository;
    private final DomainRepository domainRepository;
    private final ExerciseCourseLinkRepository exerciseCourseLinkRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseCourseLinkReassignExecutor linkReassignExecutor;
    private final ExerciseAttemptReassignExecutor attemptReassignExecutor;

    @Transactional(readOnly = true)
    public @NotNull ExerciseData getById(long exerciseId) {
        return toData(findEntity(exerciseId));
    }

    @Transactional(readOnly = true)
    public @NotNull List<ExerciseSummaryData> findSummariesByCourseId(long courseId) {
        return exerciseRepository.findAllByCourseId(courseId).stream()
                .map(ExerciseDataRepository::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public @NotNull List<ExerciseSummaryData> findPublicSummaries() {
        return exerciseRepository.findAllByIsPublicTrue().stream()
                .map(ExerciseDataRepository::toSummary)
                .toList();
    }

    @Transactional
    public long create(@NotNull NewExerciseData exercise) {
        var entity = new ExerciseEntity();
        entity.setDomain(findDomain(exercise.domainId()));
        entity.setName(exercise.name());
        entity.setBackendId(exercise.backendId());
        entity.setStrategyId(exercise.strategyId());
        entity.setOptions(exercise.options());
        entity.setStages(new ArrayList<>(exercise.stages()));
        entity.setTags(joinTags(exercise.tags()));
        entity.setPublic(exercise.isPublic());
        return exerciseRepository.save(entity).getId();
    }

    @Transactional
    public long copy(long sourceExerciseId, boolean isPublic) {
        var clone = findEntity(sourceExerciseId).clone();
        clone.setPublic(isPublic);
        return exerciseRepository.save(clone).getId();
    }

    @Transactional
    public void updateCard(@NotNull ExerciseCardUpdateData card) {
        var entity = findEntity(card.id());
        entity.setName(card.name());
        entity.setDomain(findDomain(card.domainId()));
        entity.setBackendId(card.backendId());
        entity.setStrategyId(card.strategyId());
        entity.setOptions(card.options());
        entity.setStages(new ArrayList<>(card.stages()));
        entity.setTags(joinTags(card.tags()));
        exerciseRepository.save(entity);
    }

    @Transactional
    public void updateGeneratorSettings(long exerciseId, @Nullable Integer generatorThreshold,
                                        @Nullable Integer additionalQuestionsToGenerate) {
        var entity = findEntity(exerciseId);
        var options = Strict.required(entity.getOptions(), "options", "exercise " + exerciseId);
        options.setGeneratorThreshold(generatorThreshold);
        options.setGeneratorAdditionalQuestionsToGenerate(additionalQuestionsToGenerate);
        // Настройки лежат в json-колонке: Hibernate не увидит правку внутри объекта,
        // если не переприсвоить поле.
        entity.setOptions(options);
        exerciseRepository.save(entity);
    }

    @Transactional
    public void delete(long exerciseId) {
        var exercise = findEntity(exerciseId);

        if (exercise.isPublic()) {
            var courseIds = exerciseCourseLinkRepository.findCourseIdsByExerciseId(exerciseId);
            if (!courseIds.isEmpty()) {
                var clones = new ArrayList<ExerciseEntity>(courseIds.size());
                for (int i = 0; i < courseIds.size(); i++) {
                    clones.add(exercise.clone());
                }
                exerciseRepository.saveAll(clones);
                // Идентификаторы копий нужны сразу: переезд ссылок идёт мимо контекста
                // персистентности, обычным update, и не увидит неотправленных вставок.
                exerciseRepository.flush();

                var courseToCloneId = new HashMap<Long, Long>();
                for (int i = 0; i < courseIds.size(); i++) {
                    courseToCloneId.put(courseIds.get(i), clones.get(i).getId());
                }
                linkReassignExecutor.reassign(exerciseId, courseToCloneId);
                attemptReassignExecutor.reassign(exerciseId, courseToCloneId);
            }
        }

        exerciseCourseLinkRepository.deleteByExerciseId(exerciseId);
        exerciseAttemptRepository.deleteByExerciseId(exerciseId);
        exerciseRepository.deleteById(exerciseId);
    }

    private @NotNull ExerciseEntity findEntity(long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("Exercise " + exerciseId + " not found"));
    }

    private @NotNull DomainEntity findDomain(@NotNull String domainId) {
        return domainRepository.findById(domainId)
                .orElseThrow(() -> new NoSuchElementException("Domain " + domainId + " not found"));
    }

    private static @NotNull String joinTags(@NotNull List<String> tags) {
        return String.join(", ", tags);
    }

    // ---------------------------------------------------------------- маппинг

    private static @NotNull ExerciseData toData(@NotNull ExerciseEntity entity) {
        long id = Strict.required(entity.getId(), "id", "exercise");
        String owner = "exercise " + id;
        // getDomain() ленивый, но getName() — это первичный ключ домена,
        // и его прокси отдаёт сам, без запроса.
        var domain = Strict.required(entity.getDomain(), "domain", owner);
        return new ExerciseData(
                id,
                Strict.required(entity.getName(), "name", owner),
                Strict.required(domain.getName(), "domain.name", owner),
                Strict.required(entity.getBackendId(), "backendId", owner),
                Strict.required(entity.getStrategyId(), "strategyId", owner),
                Strict.required(entity.getOptions(), "options", owner),
                List.copyOf(Strict.required(entity.getStages(), "stages", owner)),
                List.copyOf(entity.getTags()),
                entity.isPublic());
    }

    private static @NotNull ExerciseSummaryData toSummary(@NotNull ExerciseEntity entity) {
        long id = Strict.required(entity.getId(), "id", "exercise");
        return new ExerciseSummaryData(
                id,
                Strict.required(entity.getName(), "name", "exercise " + id),
                entity.isPublic());
    }
}
