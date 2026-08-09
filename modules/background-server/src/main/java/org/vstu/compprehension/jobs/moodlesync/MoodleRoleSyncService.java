package org.vstu.compprehension.jobs.moodlesync;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.AuthService.CourseRoleAssignment;
import org.vstu.compprehension.common.BatchingIterator;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceTrustStatus;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.models.entities.external_system.ExternalAccountEntity;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.EducationResourceRepository;
import org.vstu.compprehension.models.repository.ExternalAccountRepository;
import org.vstu.compprehension.moodle.request.CourseCapabilityRequest;
import org.vstu.compprehension.moodle.response.MoodleCapabilityResult;
import org.vstu.compprehension.moodle.MoodleService;
import org.vstu.compprehension.moodle.response.MoodleUserRef;
import org.vstu.compprehension.moodle.MoodleWsException;
import org.vstu.compprehension.moodle.MoodleWsResult;
import org.vstu.compprehension.moodle.config.WsFuncMoodleConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.vstu.compprehension.utils.transactions.TransactionScope;
import org.vstu.compprehension.utils.transactions.TransactionScopeFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class MoodleRoleSyncService {
    private final EducationResourceRepository eduResourceRepo;
    private final ExternalAccountRepository externalAccountRepo;
    private final CourseRepository courseRepo;
    private final AuthService authService;
    private final MoodleService moodleService;
    private final WsFuncMoodleConfig wsFuncMoodleConfig;
    private final MoodleSyncConfig syncConfig;
    private final TransactionScope transactionScope;

    public MoodleRoleSyncService(
            EducationResourceRepository eduResourceRepo,
            ExternalAccountRepository externalAccountRepo,
            CourseRepository courseRepo,
            AuthService authService,
            MoodleService moodleService,
            WsFuncMoodleConfig wsFuncMoodleConfig,
            MoodleSyncConfig syncConfig,
            TransactionScopeFactory transactionScopeFactory
    ) {
        this.eduResourceRepo = eduResourceRepo;
        this.externalAccountRepo = externalAccountRepo;
        this.courseRepo = courseRepo;
        this.authService = authService;
        this.moodleService = moodleService;
        this.wsFuncMoodleConfig = wsFuncMoodleConfig;
        this.syncConfig = syncConfig;
        this.transactionScope = transactionScopeFactory.create(TransactionScope.PropagationBehavior.REQUIRES_NEW);
    }

    public void syncAll() {
        List<EducationResourceEntity> trustedMoodles = transactionScope.execute(() ->
                eduResourceRepo.findByTypeAndTrustStatus(EducationResourceType.MOODLE, EducationResourceTrustStatus.TRUSTED));
        if (trustedMoodles.isEmpty()) {
            log.info("No trusted Moodle environments - skipping role sync");
            return;
        }

        ThreadFactory factory = Thread.ofVirtual().name("sync-job-", 0).factory();
        try (var vte = Executors.newThreadPerTaskExecutor(factory)) {
            var futures = trustedMoodles.stream()
                    .map(env -> CompletableFuture
                            .runAsync(() -> syncRolesInEnvironment(env), vte)
                            .exceptionally(ex -> {
                                log.error("sync failed for {}", env.getUrl(), ex);
                                return null;
                            }))
                    .toList();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        }
    }

    void syncRolesInEnvironment(EducationResourceEntity env) {
        log.info("Moodle role sync: starting for {}", env.getUrl());

        List<ExternalAccountEntity> accounts = transactionScope.execute(() ->
                externalAccountRepo.findByEducationResourceId(env.getId()));
        if (accounts.isEmpty()) {
            log.info("Moodle role sync: no external accounts for {}, skipping", env.getUrl());
            return;
        }

        List<CourseEntity> knownCourses = transactionScope.execute(() ->
                courseRepo.findByEducationResourceIdAndExternalCourseIdIsNotNull(env.getId()));
        if (knownCourses.isEmpty()) {
            log.info("Moodle role sync: no courses with externalCourseId for {}, skipping", env.getUrl());
            return;
        }

        String wsToken = wsFuncMoodleConfig.findByBaseUrl(env.getUrl())
                .map(r -> r.registration().getWebserviceToken())
                .orElse(null);
        if (wsToken == null) {
            log.warn("Moodle role sync: no WS-moodle registration for {} - skipping", env.getUrl());
            return;
        }

        CoursePartition partition = detachDeletedCourses(env, wsToken, knownCourses);
        List<CourseEntity> courses = partition.live();
        if (courses.isEmpty()) {
            log.info("Moodle role sync: no live Moodle courses for {} after existence check, skipping", env.getUrl());
            return;
        }

        Map<Long, Long> userIdByMoodleId = buildUserIdByMoodleIdMap(accounts);
        Map<String, CourseEntity> courseByExtId = courses.stream()
                .collect(Collectors.toMap(
                        CourseEntity::getExternalCourseId,
                        Function.identity(),
                        (a, b) -> a)
                );

        Set<String> courseCaps = CapabilityMapper.allRelevantCourseCapabilities();
        Map<Long, Map<Long, Set<String>>> userCourseCaps = new HashMap<>();

        int totalProcessed = 0;
        BatchingIterator<CourseEntity> batches = new BatchingIterator<>(courses.iterator(), syncConfig.getCoursesPerBatch());
        int batchIdx = 0;
        while (batches.hasNext()) {
            List<CourseEntity> batch = batches.next();
            List<CourseCapabilityRequest> req = new ArrayList<>();
            for (CourseEntity course : batch) {
                req.add(new CourseCapabilityRequest(course.getExternalCourseId(), courseCaps));
            }

            MoodleWsResult<List<MoodleCapabilityResult>> capabilityResult =
                    moodleService.getUsersWithCapabilityBulk(env.getUrl(), wsToken, req);
            List<MoodleCapabilityResult> resp;
            switch (capabilityResult) {
                case MoodleWsResult.Success<List<MoodleCapabilityResult>> s -> resp = s.value();
                case MoodleWsResult.Failure<List<MoodleCapabilityResult>> f -> {
                    log.warn("Moodle role sync: WS call failed for {} [{}]: {} (batch #{}, size={}) - aborting environment",
                            env.getUrl(), f.errorcode(), f.message(), batchIdx, batch.size());
                    return;
                }
            }
            if (resp.isEmpty() && syncConfig.isAbortOnEmptyResponse()) {
                log.warn("Moodle role sync: empty WS response for {} (batch #{}, size={}) - aborting environment",
                        env.getUrl(), batchIdx, batch.size());
                return;
            }
            batchIdx++;

            totalProcessed += resp.size();

            for (MoodleCapabilityResult capability : resp) {
                CourseEntity course = courseByExtId.get(capability.courseId());
                if (course == null) continue;
                for (MoodleUserRef moodleUserRef : capability.courseMembers()) {
                    Long userId = userIdByMoodleId.get(moodleUserRef.id());
                    if (userId == null) continue;
                    userCourseCaps
                            .computeIfAbsent(userId, nothing -> new HashMap<>())
                            .computeIfAbsent(course.getId(), nothing -> new HashSet<>())
                            .add(capability.capabilityName());
                }
            }
        }

        int userCourseCombos = userCourseCaps.values().stream().mapToInt(Map::size).sum();
        log.info("Moodle role sync: {} - {} WS-records processed, {} user-course combos",
                env.getUrl(), totalProcessed, userCourseCombos);

        List<CourseRoleAssignment> desiredAssignments = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Set<String>>> userEntry : userCourseCaps.entrySet()) {
            Long userId = userEntry.getKey();
            for (Map.Entry<Long, Set<String>> courseEntry : userEntry.getValue().entrySet()) {
                desiredAssignments.add(new CourseRoleAssignment(
                        userId,
                        courseEntry.getKey(),
                        CapabilityMapper.deriveCourseRole(courseEntry.getValue())
                ));
            }
        }

        Set<Long> managedCourseIds = Stream.concat(courses.stream(), partition.detached().stream())
                .map(CourseEntity::getId)
                .collect(Collectors.toSet());

        transactionScope.executeNoResult(() -> {
            if (!partition.detached().isEmpty()) {
                courseRepo.saveAll(partition.detached());
            }
            authService.reconcileCourseRoleAssignments(
                    env.getId(),
                    userIdByMoodleId.values(),
                    desiredAssignments,
                    managedCourseIds
            );
        });
    }

    /**
     * Проверяет, какие из {@code courses} ещё существуют в Moodle, и отвязывает удалённые:
     * у такого курса {@code externalCourseId} обнуляется (курс становится локальным, связь с
     * Moodle теряется) - следующий sync его уже не запросит. Возвращает только живые курсы.
     *
     * <p>Без этого шага один удалённый курс ронял весь bulk-вызов
     * {@code core_enrol_get_enrolled_users_with_capability} (он бросает
     * {@code dml_missing_record_exception} на первом отсутствующем id), и синхронизация
     *
     */
    private CoursePartition detachDeletedCourses(
            EducationResourceEntity env, String wsToken, List<CourseEntity> courses) {
        Set<String> requestedExtIds = courses.stream()
                .map(CourseEntity::getExternalCourseId)
                .collect(Collectors.toSet());

        Set<String> existingExtIds;
        try {
            existingExtIds = moodleService.findExistingCourseIds(env.getUrl(), wsToken, requestedExtIds).orElseThrow();
        } catch (MoodleWsException ex) {
            log.warn("Moodle role sync: course existence check failed for {} - aborting environment: {}",
                    env.getUrl(), ex.getMessage());
            return new CoursePartition(List.of(), List.of());
        }

        Map<Boolean, List<CourseEntity>> partition = courses.stream()
                .collect(Collectors.partitioningBy(c -> existingExtIds.contains(c.getExternalCourseId())));
        List<CourseEntity> liveCourses = partition.get(true);
        List<CourseEntity> deletedCourses = partition.get(false);

        for (CourseEntity course : deletedCourses) {
            log.info("Moodle role sync: course '{}' (extId={}) no longer exists in {} - detaching",
                    course.getName(), course.getExternalCourseId(), env.getUrl());
            course.setExternalCourseId(null);
        }
        return new CoursePartition(liveCourses, deletedCourses);
    }

    private record CoursePartition(List<CourseEntity> live, List<CourseEntity> detached) {
    }

    private Map<Long, Long> buildUserIdByMoodleIdMap(List<ExternalAccountEntity> accounts) {
        Map<Long, Long> result = new HashMap<>();
        for (var ea : accounts) {
            Long moodleId;
            try {
                moodleId = Long.parseLong(ea.getExternalId());
            } catch (NumberFormatException ignore) {
                log.warn("ExternalAccount externalId is not numeric: {} - skip", ea.getExternalId());
                continue;
            }
            result.put(moodleId, ea.getId().getUserId());
        }
        return result;
    }

}
