package org.vstu.compprehension.models.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExerciseCourseLinkReassignExecutor {
    private final JdbcTemplate jdbc;

    public void reassign(long oldExerciseId, Map<Long, Long> courseToCloneId) {
        if (courseToCloneId.isEmpty()) return;
        var batch = courseToCloneId.entrySet().stream()
                .map(e -> new Object[]{ e.getValue(), oldExerciseId, e.getKey() })
                .toList();
        jdbc.batchUpdate(
                "update exercise_course_link set exercise_id = ? where exercise_id = ? and course_id = ?",
                batch);
    }
}
