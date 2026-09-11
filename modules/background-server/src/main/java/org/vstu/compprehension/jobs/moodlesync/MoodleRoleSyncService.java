package org.vstu.compprehension.jobs.moodlesync;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.services.RoleAssignmentService;
import org.vstu.compprehension.services.RoleAssignmentService.CourseRoleAssignment;
import org.vstu.compprehension.common.BatchingIterator;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.data.cource.EducationResourceData;
import org.vstu.compprehension.data.user.ExternalAccountData;
import org.vstu.compprehension.data.cource.ExternalCourseData;
import org.vstu.compprehension.repositories.data.CourseDataRepository;
import org.vstu.compprehension.repositories.data.ExternalSystemDataRepository;
import org.vstu.compprehension.moodle.request.CourseCapabilityRequest;
import org.vstu.compprehension.moodle.response.MoodleCapabilityResult;
import org.vstu.compprehension.moodle.MoodleClient;
import org.vstu.compprehension.moodle.MoodleClientFactory;
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
    private final ExternalSystemDataRepository externalSystems;
    private final CourseDataRepository courses;
    private final RoleAssignmentService roleAssignmentService;
    private final MoodleClientFactory moodleClientFactory;
    private final WsFuncMoodleConfig wsFuncMoodleConfig;
    private final MoodleSyncConfig syncConfig;
    private final TransactionScope transactionScope;

    public MoodleRoleSyncService(
            ExternalSystemDataRepository externalSystems,
            CourseDataRepository courses,
            RoleAssignmentService roleAssignmentService,
            MoodleClientFactory moodleClientFactory,
            WsFuncMoodleConfig wsFuncMoodleConfig,
            MoodleSyncConfig syncConfig,
            TransactionScopeFactory transactionScopeFactory
    ) {
        this.externalSystems = externalSystems;
        this.courses = courses;
        this.roleAssignmentService = roleAssignmentService;
        this.moodleClientFactory = moodleClientFactory;
        this.wsFuncMoodleConfig = wsFuncMoodleConfig;
        this.syncConfig = syncConfig;
        this.transactionScope = transactionScopeFactory.create(TransactionScope.PropagationBehavior.REQUIRES_NEW);
    }

    public void syncAll() {
        List<EducationResourceData> trustedMoodles = externalSystems.findEducationResources(
                EducationResourceType.MOODLE, EducationResourceTrustStatus.TRUSTED);
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
                                log.error("sync failed for {}", env.url(), ex);
                                return null;
                            }))
                    .toList();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        }
    }

    void syncRolesInEnvironment(EducationResourceData env) {
        log.info("Moodle role sync: starting for {}", env.url());

        List<ExternalAccountData> accounts = externalSystems.findExternalAccounts(env.id());
        if (accounts.isEmpty()) {
            log.info("Moodle role sync: no external accounts for {}, skipping", env.url());
            return;
        }

        List<ExternalCourseData> knownCourses = courses.findExternalCourses(env.id());
        if (knownCourses.isEmpty()) {
            log.info("Moodle role sync: no courses with externalCourseId for {}, skipping", env.url());
            return;
        }

        String wsToken = wsFuncMoodleConfig.findByBaseUrl(env.url())
                .map(r -> r.registration().getWebserviceToken())
                .orElse(null);
        if (wsToken == null) {
            log.warn("Moodle role sync: no WS-moodle registration for {} - skipping", env.url());
            return;
        }

        MoodleClient moodleClient = moodleClientFactory.create(env.url(), wsToken);
        CoursePartition partition = detachDeletedCourses(env, moodleClient, knownCourses);
        List<ExternalCourseData> liveCourses = partition.live();
        if (liveCourses.isEmpty()) {
            log.info("Moodle role sync: no live Moodle courses for {} after existence check, skipping", env.url());
            return;
        }

        Map<Long, Long> userIdByMoodleId = buildUserIdByMoodleIdMap(accounts);
        Map<String, ExternalCourseData> courseByExtId = liveCourses.stream()
                .collect(Collectors.toMap(
                        ExternalCourseData::externalCourseId,
                        Function.identity(),
                        (a, b) -> a)
                );

        Set<String> courseCaps = MoodleCapabilities.allRelevantCourseCapabilities();
        Map<Long, Map<Long, Set<String>>> userCourseCaps = new HashMap<>();

        int totalProcessed = 0;
        BatchingIterator<ExternalCourseData> batches = new BatchingIterator<>(liveCourses.iterator(), syncConfig.getCoursesPerBatch());
        int batchIdx = 0;
        while (batches.hasNext()) {
            List<ExternalCourseData> batch = batches.next();
            List<CourseCapabilityRequest> req = new ArrayList<>();
            for (ExternalCourseData course : batch) {
                req.add(new CourseCapabilityRequest(course.externalCourseId(), courseCaps));
            }

            MoodleWsResult<List<MoodleCapabilityResult>> capabilityResult =
                    moodleClient.getUsersWithCapabilityBulk(req);
            List<MoodleCapabilityResult> resp;
            switch (capabilityResult) {
                case MoodleWsResult.Success<List<MoodleCapabilityResult>> s -> resp = s.value();
                case MoodleWsResult.Failure<List<MoodleCapabilityResult>> f -> {
                    log.warn("Moodle role sync: WS call failed for {} [{}]: {} (batch #{}, size={}) - aborting environment",
                            env.url(), f.errorcode(), f.message(), batchIdx, batch.size());
                    return;
                }
            }
            if (resp.isEmpty() && syncConfig.isAbortOnEmptyResponse()) {
                log.warn("Moodle role sync: empty WS response for {} (batch #{}, size={}) - aborting environment",
                        env.url(), batchIdx, batch.size());
                return;
            }
            batchIdx++;

            totalProcessed += resp.size();

            for (MoodleCapabilityResult capability : resp) {
                ExternalCourseData course = courseByExtId.get(capability.courseId());
                if (course == null) continue;
                for (MoodleUserRef moodleUserRef : capability.courseMembers()) {
                    Long userId = userIdByMoodleId.get(moodleUserRef.id());
                    if (userId == null) continue;
                    userCourseCaps
                            .computeIfAbsent(userId, nothing -> new HashMap<>())
                            .computeIfAbsent(course.id(), nothing -> new HashSet<>())
                            .add(capability.capabilityName());
                }
            }
        }

        int userCourseCombos = userCourseCaps.values().stream().mapToInt(Map::size).sum();
        log.info("Moodle role sync: {} - {} WS-records processed, {} user-course combos",
                env.url(), totalProcessed, userCourseCombos);

        List<CourseRoleAssignment> desiredAssignments = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Set<String>>> userEntry : userCourseCaps.entrySet()) {
            Long userId = userEntry.getKey();
            for (Map.Entry<Long, Set<String>> courseEntry : userEntry.getValue().entrySet()) {
                desiredAssignments.add(new CourseRoleAssignment(
                        userId,
                        courseEntry.getKey(),
                        MoodleCapabilities.deriveCourseRole(courseEntry.getValue())
                ));
            }
        }

        Set<Long> managedCourseIds = Stream.concat(liveCourses.stream(), partition.detached().stream())
                .map(ExternalCourseData::id)
                .collect(Collectors.toSet());

        transactionScope.executeNoResult(() -> {
            courses.detachFromExternalSystem(
                    partition.detached().stream().map(ExternalCourseData::id).toList());
            roleAssignmentService.reconcileCourseRoleAssignments(
                    env.id(),
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
            EducationResourceData env, MoodleClient moodleClient, List<ExternalCourseData> knownCourses) {
        Set<String> requestedExtIds = knownCourses.stream()
                .map(ExternalCourseData::externalCourseId)
                .collect(Collectors.toSet());

        Set<String> existingExtIds;
        try {
            existingExtIds = moodleClient.findExistingCourseIds(requestedExtIds).orElseThrow();
        } catch (MoodleWsException ex) {
            log.warn("Moodle role sync: course existence check failed for {} - aborting environment: {}",
                    env.url(), ex.getMessage());
            return new CoursePartition(List.of(), List.of());
        }

        Map<Boolean, List<ExternalCourseData>> partition = knownCourses.stream()
                .collect(Collectors.partitioningBy(c -> existingExtIds.contains(c.externalCourseId())));
        List<ExternalCourseData> liveCourses = partition.get(true);
        List<ExternalCourseData> deletedCourses = partition.get(false);

        for (ExternalCourseData course : deletedCourses) {
            log.info("Moodle role sync: course '{}' (extId={}) no longer exists in {} - detaching",
                    course.name(), course.externalCourseId(), env.url());
        }
        return new CoursePartition(liveCourses, deletedCourses);
    }

    private record CoursePartition(List<ExternalCourseData> live, List<ExternalCourseData> detached) {
    }

    private Map<Long, Long> buildUserIdByMoodleIdMap(List<ExternalAccountData> accounts) {
        Map<Long, Long> result = new HashMap<>();
        for (var account : accounts) {
            Long moodleId;
            try {
                moodleId = Long.parseLong(account.externalId());
            } catch (NumberFormatException ignore) {
                log.warn("ExternalAccount externalId is not numeric: {} - skip", account.externalId());
                continue;
            }
            result.put(moodleId, account.userId());
        }
        return result;
    }

}
