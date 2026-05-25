package org.vstu.compprehension.jobs.moodlesync;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.AuthService.UserCourseKey;
import org.vstu.compprehension.common.BatchingIterator;
import org.vstu.compprehension.config.WsFuncMoodleConfig;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceTrustStatus;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;
import org.vstu.compprehension.models.entities.course.ExternalAccountEntity;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.EducationResourceRepository;
import org.vstu.compprehension.models.repository.ExternalAccountRepository;
import org.vstu.compprehension.integration.moodle.CourseCapabilityRequest;
import org.vstu.compprehension.integration.moodle.MoodleCapabilityResult;
import org.vstu.compprehension.integration.moodle.MoodleService;
import org.vstu.compprehension.integration.moodle.MoodleUserRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public MoodleRoleSyncService(
            EducationResourceRepository eduResourceRepo,
            ExternalAccountRepository externalAccountRepo,
            CourseRepository courseRepo,
            AuthService authService,
            MoodleService moodleService,
            WsFuncMoodleConfig wsFuncMoodleConfig,
            MoodleSyncConfig syncConfig
    ) {
        this.eduResourceRepo = eduResourceRepo;
        this.externalAccountRepo = externalAccountRepo;
        this.courseRepo = courseRepo;
        this.authService = authService;
        this.moodleService = moodleService;
        this.wsFuncMoodleConfig = wsFuncMoodleConfig;
        this.syncConfig = syncConfig;
    }

    public void syncAll() {
        List<EducationResourceEntity> trustedMoodles =
                eduResourceRepo.findByTypeAndTrustStatus(EducationResourceType.MOODLE, EducationResourceTrustStatus.TRUSTED);
        if (trustedMoodles.isEmpty()) {
            log.info("No trusted Moodle environments — skipping role sync");
            return;
        }

        try (var vte = Executors.newVirtualThreadPerTaskExecutor()) {
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

        List<ExternalAccountEntity> accounts = externalAccountRepo.findByEducationResourceId(env.getId());
        if (accounts.isEmpty()) {
            log.info("Moodle role sync: no external accounts for {}, skipping", env.getUrl());
            return;
        }

        List<CourseEntity> courses = courseRepo.findByEducationResourceIdAndExternalCourseIdIsNotNull(env.getId());
        if (courses.isEmpty()) {
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

        Map<Long, Long> userIdByMoodleId = buildUserIdByMoodleIdMap(accounts);
        Map<String, CourseEntity> courseByExtId = courses.stream()
                .collect(Collectors.toMap(
                        CourseEntity::getExternalCourseId,
                        Function.identity(),
                        (a, b) -> a)
                );

        Set<String> courseCaps = CapabilityMapper.allRelevantCourseCapabilities();
        Map<UserCourseKey, Set<String>> userCourseCaps = new HashMap<>();

        int totalProcessed = 0;
        BatchingIterator<CourseEntity> batches = new BatchingIterator<>(courses.iterator(), syncConfig.getCoursesPerBatch());
        int batchIdx = 0;
        while (batches.hasNext()) {
            List<CourseEntity> batch = batches.next();
            List<CourseCapabilityRequest> req = new ArrayList<>();
            for (CourseEntity course : batch) {
                req.add(new CourseCapabilityRequest(course.getExternalCourseId(), courseCaps));
            }

            List<MoodleCapabilityResult> resp = moodleService.getUsersWithCapabilityBulk(env.getUrl(), wsToken, req);
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
                    userCourseCaps.computeIfAbsent(
                                    new UserCourseKey(userId, course.getId()),
                                    nothing -> new HashSet<>()
                            )
                            .add(capability.capabilityName());
                }
            }
        }

        log.info("Moodle role sync: {} — {} WS-records processed, {} user-course combos",
                env.getUrl(), totalProcessed, userCourseCaps.size());

        Map<UserCourseKey, Role> desiredCourseRoles = new HashMap<>();
        for (Map.Entry<UserCourseKey, Set<String>> entry : userCourseCaps.entrySet()) {
            desiredCourseRoles.put(entry.getKey(), CapabilityMapper.deriveCourseRole(entry.getValue()));
        }

        authService.applyEnvironmentAssignmentsDiff(
                env.getId(),
                userIdByMoodleId.values(),
                desiredCourseRoles
        );
    }

    private Map<Long, Long> buildUserIdByMoodleIdMap(List<ExternalAccountEntity> accounts) {
        Map<Long, Long> result = new HashMap<>();
        for (var ea : accounts) {
            Long moodleId;
            try {
                moodleId = Long.parseLong(ea.getExternalId());
            } catch (NumberFormatException ignore) {
                log.warn("ExternalAccount externalId is not numeric: {} — skip", ea.getExternalId());
                continue;
            }
            result.put(moodleId, ea.getUser().getId());
        }
        return result;
    }

}
