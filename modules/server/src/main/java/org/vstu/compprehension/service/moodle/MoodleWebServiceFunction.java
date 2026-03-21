package org.vstu.compprehension.service.moodle;

import lombok.Getter;

/**
 * Перечисление функций Moodle Web Services REST API.
 */
@Getter
public enum MoodleWebServiceFunction {
    GET_USERS_BY_FIELD("core_user_get_users_by_field"),
    GET_USERS_COURSES("core_enrol_get_users_courses"),
    GET_LTIS_BY_COURSES("mod_lti_get_ltis_by_courses"),
    UPDATE_GRADES("core_grades_update_grades");

    private final String wsFunctionName;

    MoodleWebServiceFunction(String wsFunctionName) {
        this.wsFunctionName = wsFunctionName;
    }
}
