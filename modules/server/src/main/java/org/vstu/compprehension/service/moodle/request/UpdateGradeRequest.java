package org.vstu.compprehension.service.moodle.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Запрос core_grades_update_grades.
 * Константы сервиса (source, component, itemnumber) добавляются в {@code MoodleService}.
 */
@Data
@Builder
public class UpdateGradeRequest implements MoodleRequest {
    @JsonProperty("courseid")
    private long courseId;
    @JsonProperty("activityid")
    private long courseModuleId;
    @JsonProperty("grades")
    private List<GradeItem> grades;
}
