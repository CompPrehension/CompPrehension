package org.vstu.compprehension.moodle;

import org.vstu.compprehension.moodle.request.CourseCapabilityRequest;
import org.vstu.compprehension.moodle.request.MoodleGrade;
import org.vstu.compprehension.moodle.response.MoodleCapabilityResult;
import org.vstu.compprehension.moodle.response.MoodleLtiActivity;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Клиент Moodle WS REST API.
 */
public interface MoodleClient {

    /**
     * Bulk-запрос: для каждого {@link CourseCapabilityRequest#externalCourseId} проверяет
     * перечисленные {@link CourseCapabilityRequest#capabilities} и возвращает список юзеров.
     *
     * <p>Возвращает плоский список {@link MoodleCapabilityResult} по парам (courseId, capabilityName).
     */
    MoodleWsResult<List<MoodleCapabilityResult>> getUsersWithCapabilityBulk(List<CourseCapabilityRequest> requests);

    /**
     * Возвращает подмножество {@code courseIds}, реально существующих в Moodle.
     */
    MoodleWsResult<Set<String>> findExistingCourseIds(Collection<String> courseIds);

    /**
     * LTI-активности курса.
     */
    MoodleWsResult<List<MoodleLtiActivity>> getLtiActivitiesInCourse(String moodleCourseId);

    /**
     * Записывает {@link MoodleGrade} в колонку журнала активности {@code mod_lti}
     * ({@code courseId} + {@code courseModuleId}) для студента {@code studentId}
     * через {@code core_grades_update_grades}.
     * @return {@link MoodleWsResult.Success} с {@code true}, если Moodle вернул код {@code 0}
     * (GRADE_UPDATE_OK); {@code Success(false)} — на любой иной код
     */
    MoodleWsResult<Boolean> updateGradeInCourse(
            String externalCourseId,
            long courseModuleId,
            String studentId,
            MoodleGrade grade
    );
}
