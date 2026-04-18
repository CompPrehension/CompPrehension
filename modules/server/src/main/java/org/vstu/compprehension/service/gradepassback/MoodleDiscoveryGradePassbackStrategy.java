package org.vstu.compprehension.service.gradepassback;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.vstu.compprehension.config.MoodleWsRegistrationsProperties;
import org.vstu.compprehension.config.MoodleWsRegistrationsProperties.Registration;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.educationresource.EducationResourceType;
import org.vstu.compprehension.models.entities.externalaccount.MoodleAccountEntity;
import org.vstu.compprehension.models.repository.MoodleAccountRepository;
import org.vstu.compprehension.service.moodle.MoodleService;
import org.vstu.compprehension.service.moodle.request.GradeItem;
import org.vstu.compprehension.service.moodle.request.UpdateGradeRequest;
import org.vstu.compprehension.service.moodle.response.MoodleCourseResponse;
import org.vstu.compprehension.service.moodle.response.MoodleLtiActivityResponse;

import java.util.List;
import java.util.Map;

/**
 * Grade passback через Moodle Web Services для пользователей без LTI-контекста
 * (прямой доступ через Keycloak). Отключено по умолчанию через
 * {@code compprehension.grade-passback.moodle-ws.enabled}:
 * */
@Service
@ConditionalOnProperty(
        prefix = "compprehension.grade-passback.moodle-ws",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Log4j2
public class MoodleDiscoveryGradePassbackStrategy implements GradePassbackStrategy {

    private final MoodleService moodleService;
    private final MoodleWsRegistrationsProperties moodleWsRegistrations;
    private final MoodleAccountRepository moodleAccountRepository;

    @Override
    public boolean supports(ExerciseAttemptEntity attempt) {
        return attempt.getLtiContext() == null;
    }

    @Override
    public void passGrade(ExerciseAttemptEntity attempt) {
        UserEntity user = attempt.getUser();
        Long userId = user.getId();
        Long exerciseId = attempt.getExercise().getId();

        var accounts = user.getAccountsByType(EducationResourceType.MOODLE)
                .map(a -> (MoodleAccountEntity) a)
                .toList();
        if (accounts.isEmpty()) {
            log.debug("Grade passback skipped for attempt {}: user {} has no linked MoodleAccount",
                    attempt.getId(), userId);
            return;
        }

        for (MoodleAccountEntity account : accounts) {
            String baseUrl = account.getEducationResource().getUrl();
            var regOpt = moodleWsRegistrations.findByBaseUrl(baseUrl);
            if (regOpt.isEmpty()) {
                log.debug("Grade passback attempt {}: no WS registration for {}, skipping tenant",
                        attempt.getId(), baseUrl);
                continue;
            }
            if (tryPassGrade(attempt, regOpt.get(), account.getMoodleUserId(), exerciseId)) {
                return;
            }
        }
        log.warn("Grade passback skipped for attempt {}: no Moodle with matching activity for exercise_id={} " +
                        "across {} linked account(s) of user {}",
                attempt.getId(), exerciseId, accounts.size(), userId);
    }

    /**
     * Пытается отправить оценку через указанный Moodle. Возвращает true, если удалось.
     */
    private boolean tryPassGrade(ExerciseAttemptEntity attempt, Registration reg, Long moodleUserId, Long exerciseId) {
        List<Long> courseIds = moodleService.getEnrolledCourses(reg, moodleUserId)
                .stream().map(MoodleCourseResponse::getId).toList();
        if (courseIds.isEmpty()) return false;

        List<MoodleLtiActivityResponse> matching = moodleService.getLtiActivities(reg, courseIds).stream()
                .filter(a -> exerciseId.equals(extractExerciseId(a.getToolUrl(), a.getCustomParams())))
                .toList();
        if (matching.isEmpty()) return false;

        if (matching.size() > 1) {
            log.debug("Attempt {}: {} LTI activities for exercise_id={} in Moodle '{}', posting to first",
                    attempt.getId(), matching.size(), exerciseId, reg.getBaseUrl());
        }
        MoodleLtiActivityResponse activity = matching.get(0);
        double grade = calculateFinalGrade(attempt);

        try {
            moodleService.updateGrade(reg, UpdateGradeRequest.builder()
                    .courseId(activity.getCourseId())
                    .courseModuleId(activity.getCmid())
                    .grades(List.of(GradeItem.builder()
                            .moodleUserId(String.valueOf(moodleUserId))
                            .rawGrade(grade * 100)
                            .build()))
                    .build());
            log.info("Moodle grade posted for attempt {}: moodle='{}', courseId={}, cmid={}, userId={}, grade={}",
                    attempt.getId(), reg.getBaseUrl(), activity.getCourseId(), activity.getCmid(), moodleUserId, grade);
            return true;
        } catch (Exception e) {
            log.error("Moodle grade passback failed for attempt {}, moodle '{}', course {}: {}",
                    attempt.getId(), reg.getBaseUrl(), activity.getCourseId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Извлекает exercise_id из customParams ({@code exercise_id}/{@code custom_exercise_id}) или из query-параметра toolUrl ({@code exercise_id}/{@code id}).
     */
    private Long extractExerciseId(String toolUrl, Map<String, String> customParams) {
        String raw = customParams.getOrDefault("exercise_id", customParams.get("custom_exercise_id"));

        if (raw == null && toolUrl != null) {
            MultiValueMap<String, String> query =
                    UriComponentsBuilder.fromUriString(toolUrl).build().getQueryParams();
            raw = query.getFirst("exercise_id");
            if (raw == null) raw = query.getFirst("id");
        }

        if (raw != null) {
            try {
                return Long.parseLong(raw.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
