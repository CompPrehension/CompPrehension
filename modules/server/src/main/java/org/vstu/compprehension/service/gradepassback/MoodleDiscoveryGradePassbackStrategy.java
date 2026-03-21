package org.vstu.compprehension.service.gradepassback;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.service.moodle.MoodleService;
import org.vstu.compprehension.service.moodle.request.GradeItem;
import org.vstu.compprehension.service.moodle.request.UpdateGradeRequest;
import org.vstu.compprehension.service.moodle.response.MoodleCourseResponse;
import org.vstu.compprehension.service.moodle.response.MoodleLtiActivityResponse;

import java.util.List;
import java.util.Map;

/**
 * Стратегия grade passback через Moodle Web Services REST API.
 *
 * <p><b>Когда применяется.</b> Только для пользователей, аутентифицированных через Keycloak
 * (прямой доступ к тренажёру, без LTI-запуска из Moodle). Признак — отсутствие
 * {@code ltiLineitemUrl} у попытки. Работает через токен администратора Moodle,
 * а не через сессию студента, поэтому студент может никогда не заходить в Moodle —
 * достаточно, чтобы его учётная запись там существовала.
 *
 * <p><b>Почему ищется именно LTI-активность.</b> CompPrehension встроен в Moodle исключительно
 * через элемент курса "External Tool" ({@code mod_lti}). Преподаватель добавляет его
 * в курс и указывает Tool URL с {@code ?id=N}, где N — идентификатор упражнения.
 * Других способов связать упражнение с курсом в текущей архитектуре нет.
 *
 * <p><b>Алгоритм:</b>
 * <ol>
 *   <li>Ищет пользователя в Moodle по email ({@code core_user_get_users_by_field}).
 *       Если не найден — passback пропускается (warn в лог).</li>
 *   <li>Получает список курсов, на которые записан пользователь
 *       ({@code core_enrol_get_users_courses}).
 *       Если курсов нет — пропускается (debug в лог).</li>
 *   <li>Получает все LTI-активности из этих курсов ({@code mod_lti_get_ltis_by_courses})
 *       и фильтрует те, чей {@code exercise_id} совпадает с упражнением попытки
 *       (ищется в query-параметре Tool URL или в custom params активности).
 *       Если подходящих нет — пропускается (debug в лог).</li>
 *   <li>Выставляет оценку в первую найденную активность ({@code core_grades_update_grades}).
 *       <!-- TODO: если студент записан на несколько курсов с одним упражнением,
 *            оценка идёт только в первый. Для точного выбора курса нужен контекст
 *            со стороны фронта (например, courseId, переданный при старте попытки). --></li>
 * </ol>
 *
 * <p><b>Условия успешной отправки:</b>
 * <ul>
 *   <li>Email в Keycloak совпадает с email в Moodle.</li>
 *   <li>Студент записан хотя бы на один курс в Moodle.</li>
 *   <li>В курсе существует LTI-активность, ссылающаяся на нужное упражнение.</li>
 * </ul>
 */
@Service
@Order(2)
@RequiredArgsConstructor
@Log4j2
public class MoodleDiscoveryGradePassbackStrategy implements GradePassbackStrategy {

    private final MoodleService moodleService;
    private final ExerciseAttemptService exerciseAttemptService;

    @Override
    public boolean supports(ExerciseAttemptEntity attempt) {
        return attempt.getLtiLineitemUrl() == null && moodleService.isConfigured();
    }

    @Override
    public void passGrade(ExerciseAttemptEntity attempt) {
        String email = attempt.getUser().getEmail();
        Long exerciseId = attempt.getExercise().getId();

        var userOpt = moodleService.findUserByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Grade passback skipped for attempt {}: user '{}' not found in Moodle by email",
                    attempt.getId(), email);
            return;
        }
        long moodleUserId = userOpt.get().getId();

        List<Long> courseIds = moodleService.getEnrolledCourses(moodleUserId)
                .stream().map(MoodleCourseResponse::getId).toList();
        if (courseIds.isEmpty()) {
            log.debug("Grade passback skipped for attempt {}: user '{}' has no enrolled courses in Moodle",
                    attempt.getId(), email);
            return;
        }

        List<MoodleLtiActivityResponse> allActivities = moodleService.getLtiActivities(courseIds);
        List<MoodleLtiActivityResponse> matchingActivities = allActivities.stream()
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

        MoodleLtiActivityResponse activity = matchingActivities.get(0);
        double grade = exerciseAttemptService.calculateFinalGrade(attempt);

        try {
            moodleService.updateGrade(UpdateGradeRequest.builder()
                    .courseId(activity.getCourseId())
                    .courseModuleId(activity.getCmid())
                    .grades(List.of(GradeItem.builder()
                            .moodleUserId(String.valueOf(moodleUserId))
                            .rawGrade(grade * 100)
                            .build()))
                    .build());
            log.info("Moodle grade posted for attempt {}: courseId={}, cmid={}, userId={}, grade={}",
                    attempt.getId(), activity.getCourseId(), activity.getCmid(), moodleUserId, grade);
        } catch (Exception e) {
            log.error("Moodle grade passback failed for attempt {}, course {}: {}",
                    attempt.getId(), activity.getCourseId(), e.getMessage(), e);
        }
    }

    /**
     * Извлекает exercise_id из custom params или URL External Tool LTI-активности.
     *
     * <p>Проверяет два источника:
     * <ul>
     *   <li>customParams: ключи {@code exercise_id} или {@code custom_exercise_id}</li>
     *   <li>toolUrl: query-параметр {@code exercise_id} или {@code id}</li>
     * </ul>
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
