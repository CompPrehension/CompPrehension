package org.vstu.compprehension.service.moodle.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Запрос mod_lti_get_ltis_by_courses.
 * {@code courseIds} раскрывается в {@code courseids[0]=X, courseids[1]=Y, ...}
 */
@Data
@Builder
public class GetLtiActivitiesRequest implements MoodleRequest {
    @JsonProperty("courseids")
    private List<Long> courseIds;
}
