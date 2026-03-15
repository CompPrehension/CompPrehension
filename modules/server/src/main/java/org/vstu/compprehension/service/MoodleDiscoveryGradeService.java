package org.vstu.compprehension.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

import java.util.List;

/**
 * Выставляет оценку в Moodle через Web Services REST API для пользователей,
 * аутентифицированных через Keycloak (без LTI-запуска).
 *
 * <p>Алгоритм:
 * <ol>
 *   <li>Ищет пользователя в Moodle по email (core_user_get_users_by_field)</li>
 *   <li>Получает список курсов пользователя (core_enrol_get_users_courses)</li>
 *   <li>Находит LTI-активности с нужным exercise_id в toolurl или custom params
 *       (mod_lti_get_ltis_by_courses)</li>
 *   <li>Выставляет оценку в первый найденный курс (core_grades_update_grades)</li>
 * </ol>
 *
 * <p>Требования:
 * <ul>
 *   <li>Преподаватель создал LTI-активность в Moodle с exercise_id в toolurl (?id=N)
 *       или в custom parameters (exercise_id=N)</li>
 *   <li>Студент зачислён в этот курс в Moodle</li>
 *   <li>Email студента в Moodle совпадает с email в Keycloak</li>
 *   <li>moodle.base-url и moodle.webservice-token настроены в application.properties</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class MoodleDiscoveryGradeService {

    private final MoodleApiClient moodleApiClient;
    private final ExerciseAttemptService exerciseAttemptService;

    /**
     * Находит Moodle-курс пользователя с данным упражнением и выставляет оценку
     * через Moodle Web Services REST API.
     *
     * <p>Если пользователь записан в несколько курсов с одним упражнением — оценка
     * отправляется только в первый найденный курс.
     *
     * <p>Ошибки логируются и не пробрасываются — grade passback не должен ломать flow.
     */
    public void postGradeIfEnrolled(ExerciseAttemptEntity attempt) {
        if (!moodleApiClient.isConfigured()) {
            log.debug("Moodle Web Services not configured, grade passback skipped for attempt {}",
                    attempt.getId());
            return;
        }

        var email = attempt.getUser().getEmail();
        var exerciseId = attempt.getExercise().getId();

        var moodleUserIdOpt = moodleApiClient.findUserIdByEmail(email);
        if (moodleUserIdOpt.isEmpty()) {
            log.warn("Grade passback skipped for attempt {}: user '{}' not found in Moodle by email",
                    attempt.getId(), email);
            return;
        }
        var moodleUserId = moodleUserIdOpt.get();

        var courseIds = moodleApiClient.getEnrolledCourseIds(moodleUserId);
        if (courseIds.isEmpty()) {
            log.debug("Grade passback skipped for attempt {}: user '{}' has no enrolled courses in Moodle",
                    attempt.getId(), email);
            return;
        }

        var allActivities = moodleApiClient.getLtiActivities(courseIds);
        var matchingActivities = allActivities.stream()
                .filter(a -> exerciseId.equals(extractExerciseId(a.getToolUrl(), a.getCustomParams())))
                .toList();

        if (matchingActivities.isEmpty()) {
            log.debug("Grade passback skipped for attempt {}: no Moodle LTI activity found " +
                            "for exercise_id={} in {} enrolled course(s)",
                    attempt.getId(), exerciseId, courseIds.size());
            return;
        }

        if (matchingActivities.size() > 1) {
            log.debug("Attempt {}: {} LTI activities found for exercise_id={}, posting to first",
                    attempt.getId(), matchingActivities.size(), exerciseId);
        }

        var activity = matchingActivities.get(0);
        var grade = exerciseAttemptService.calculateFinalGrade(attempt);

        try {
            moodleApiClient.updateGrade(activity.getCourseId(), activity.getCmid(),
                    moodleUserId, grade * 100);
            log.info("Moodle grade posted for attempt {}: courseId={}, cmid={}, userId={}, grade={}",
                    attempt.getId(), activity.getCourseId(), activity.getCmid(), moodleUserId, grade);
        } catch (Exception e) {
            log.error("Moodle grade passback failed for attempt {}, course {}: {}",
                    attempt.getId(), activity.getCourseId(), e.getMessage(), e);
        }
    }

    /**
     * Извлекает exercise_id из URL External Tool или custom parameters LTI-активности.
     *
     * <p>Проверяет два источника:
     * <ul>
     *   <li>toolUrl: query-параметр {@code id} или {@code exercise_id}, например
     *       {@code https://trainer/lti/1_3/exercise?id=3}</li>
     *   <li>customParams: строка key=value (разделитель \n или ;), например
     *       {@code exercise_id=3}</li>
     * </ul>
     */
    private Long extractExerciseId(String toolUrl, String customParams) {
        // Пробуем из toolurl (?id=N или ?exercise_id=N)
        if (toolUrl != null && toolUrl.contains("?")) {
            var query = toolUrl.split("\\?", 2)[1];
            for (var param : query.split("&")) {
                var kv = param.split("=", 2);
                if (kv.length == 2) {
                    var key = kv[0].trim();
                    if ("id".equals(key) || "exercise_id".equalsIgnoreCase(key)) {
                        try { return Long.parseLong(kv[1].trim()); } catch (NumberFormatException ignored) { }
                    }
                }
            }
        }

        // Пробуем из instructorcustomparameters (key=value, разделитель \n или ;)
        if (customParams != null && !customParams.isEmpty()) {
            for (var line : customParams.split("[;\n]")) {
                var kv = line.split("=", 2);
                if (kv.length == 2 && "exercise_id".equalsIgnoreCase(kv[0].trim())) {
                    try { return Long.parseLong(kv[1].trim()); } catch (NumberFormatException ignored) { }
                }
            }
        }

        return null;
    }
}
