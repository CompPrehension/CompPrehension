package org.vstu.compprehension.service.moodle.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Элемент массива grades[] в запросе core_grades_update_grades.
 */
@Data
@Builder
public class GradeItem {
    @JsonProperty("studentid")
    private String moodleUserId;
    @JsonProperty("grade")
    private double rawGrade;
}
