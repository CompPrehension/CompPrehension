package org.vstu.compprehension.service.gradepassback;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.vstu.compprehension.Service.ExternalAccountService;
import org.vstu.compprehension.moodle.request.MoodleGrade;
import org.vstu.compprehension.moodle.response.MoodleLtiActivity;
import org.vstu.compprehension.moodle.MoodleService;
import org.vstu.compprehension.moodle.MoodleWsResult;
import org.vstu.compprehension.moodle.config.WsFuncMoodleConfig;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.models.entities.external_system.ExternalAccountEntity;

import java.util.List;
import java.util.Optional;

/**
 * Grade passback через Moodle Web Services. Применяется при прямом входе
 * (Keycloak), когда у попытки нет {@code ltiLineitemUrl} и AGS неприменим.
 *
 * <p>Адресат оценки восстанавливается из двух сохранённых идентификаторов —
 * Moodle-id пользователя (связь учётной записи {@code external_account}) и
 * внешнего id курса. Колонка журнала (instance id активности {@code mod_lti})
 * обнаруживается на лету через {@code mod_lti_get_ltis_by_courses} и
 * сопоставляется с упражнением по custom-параметру {@code exercise_id}.
 */
@Service
@Order(2)
@Log4j2
@RequiredArgsConstructor
public class MoodleDiscoveryGradePassbackStrategy implements GradePassbackStrategy {

    /** Шкала колонки журнала mod_lti по умолчанию, если активность не вернула grademax. */
    private static final double DEFAULT_GRADE_MAX = 100.0;

    private final MoodleService moodleService;
    private final WsFuncMoodleConfig wsFuncMoodleConfig;
    private final ExternalAccountService externalAccountService;

    @Override
    public boolean supports(ExerciseAttemptEntity attempt) {
        // Взаимоисключающе с AGS: если есть lineitemUrl — оценку отправит LtiAgsGradePassbackStrategy.
        if (attempt.getLtiLineitemUrl() != null) {
            return false;
        }
        CourseEntity course = attempt.getCourse();
        if (course == null || course.getExternalCourseId() == null) {
            return false;
        }
        EducationResourceEntity eduRes = course.getEducationResource();
        if (eduRes == null || eduRes.getType() != EducationResourceType.MOODLE) {
            return false;
        }
        return wsFuncMoodleConfig.findByBaseUrl(eduRes.getUrl()).isPresent();
    }

    @Override
    public boolean passGrade(ExerciseAttemptEntity attempt, double grade) {
        CourseEntity course = attempt.getCourse();
        EducationResourceEntity eduRes = course.getEducationResource();
        String baseUrl = eduRes.getUrl();
        String externalCourseId = course.getExternalCourseId();

        String wsToken = wsFuncMoodleConfig.findByBaseUrl(baseUrl)
                .map(r -> r.registration().getWebserviceToken())
                .orElse(null);
        if (wsToken == null) {
            log.warn("No WS-moodle registration for {} — cannot pass grade for attempt {}",
                    baseUrl, attempt.getId());
            return false;
        }

        Optional<ExternalAccountEntity> account = externalAccountService
                .findByUserAndEducationResource(attempt.getUser().getId(), eduRes.getId());
        if (account.isEmpty()) {
            log.warn("No external account linking user {} to {} — cannot pass grade for attempt {}",
                    attempt.getUser().getId(), baseUrl, attempt.getId());
            return false;
        }
        String moodleUserId = account.get().getExternalId();

        long exerciseId = attempt.getExercise().getId();
        MoodleWsResult<List<MoodleLtiActivity>> ltiActivitiesResult =
                moodleService.getLtiActivitiesInCourse(baseUrl, wsToken, externalCourseId);
        List<MoodleLtiActivity> activities;
        switch (ltiActivitiesResult) {
            case MoodleWsResult.Success<List<MoodleLtiActivity>> s -> activities = s.value();
            case MoodleWsResult.Failure<List<MoodleLtiActivity>> f -> {
                log.warn("Failed to fetch mod_lti activities for course {} ({}) [{}]: {} — cannot pass grade for attempt {}",
                        externalCourseId, baseUrl, f.errorcode(), f.message(), attempt.getId());
                return false;
            }
        }
        Optional<MoodleLtiActivity> activity = activities.stream()
                .filter(a -> matchesExercise(a, exerciseId))
                .findFirst();
        if (activity.isEmpty()) {
            log.warn("No mod_lti activity for exercise {} in course {} ({}) — cannot pass grade for attempt {}",
                    exerciseId, externalCourseId, baseUrl, attempt.getId());
            return false;
        }
        MoodleLtiActivity lti = activity.get();
        if (lti.getCourseModuleId() == null) {
            log.warn("mod_lti activity {} for exercise {} in course {} ({}) has no course module id — cannot pass grade for attempt {}",
                    lti.getId(), exerciseId, externalCourseId, baseUrl, attempt.getId());
            return false;
        }

        double gradeMax = lti.getGradeMax() != null && lti.getGradeMax() > 0 ? lti.getGradeMax() : DEFAULT_GRADE_MAX;
        MoodleGrade moodleGrade = new MoodleGrade(grade, gradeMax);
        MoodleWsResult<Boolean> gradeResult = moodleService.updateGradeInCourse(
                baseUrl, wsToken, externalCourseId, lti.getCourseModuleId(), moodleUserId, moodleGrade);
        return switch (gradeResult) {
            case MoodleWsResult.Success<Boolean> s -> {
                if (!s.value()) {
                    log.warn("Moodle returned non-OK code updating grade for attempt {} (course {}, {})",
                            attempt.getId(), externalCourseId, baseUrl);
                }
                yield s.value();
            }
            case MoodleWsResult.Failure<Boolean> f -> {
                log.warn("Failed to update grade for attempt {} (course {}, {}) [{}]: {}",
                        attempt.getId(), externalCourseId, baseUrl, f.errorcode(), f.message());
                yield false;
            }
        };
    }

    /**
     * Активность соответствует упражнению, если её custom-параметр {@code exercise_id}
     * (или, запасной вариант, параметр {@code exercise_id}/{@code id} в toolurl) численно
     * равен id упражнения. Сравнение точное по числу — чтобы {@code exercise_id=1} не совпало с {@code =10}.
     */
    private boolean matchesExercise(MoodleLtiActivity activity, long exerciseId) {
        Long exerciseIdFromCustom = parseLong(activity.getCustomParameters().get("exercise_id"));
        if (exerciseIdFromCustom != null) {
            return exerciseIdFromCustom == exerciseId;
        }
        UriComponents toolUrl = activity.getToolUrl();
        if (toolUrl == null) {
            return false;
        }
        MultiValueMap<String, String> query = toolUrl.getQueryParams();
        // fallback для старого варианта конфигурации, когда создавался External Tool на одно упражнение,
        // и id передавался как параметр запроса
        Long exerciseIdFromUrl = parseLong(query.getFirst("exercise_id"));
        if (exerciseIdFromUrl == null) {
            exerciseIdFromUrl = parseLong(query.getFirst("id"));
        }
        return exerciseIdFromUrl != null && exerciseIdFromUrl == exerciseId;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignore) {
            return null;
        }
    }
}
