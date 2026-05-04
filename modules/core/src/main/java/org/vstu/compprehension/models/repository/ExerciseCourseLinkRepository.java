package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkId;

@Repository
public interface ExerciseCourseLinkRepository extends JpaRepository<ExerciseCourseLinkEntity, ExerciseCourseLinkId> {

    /**
     * Inserts a link only if no row with the same ({@link ExerciseCourseLinkEntity#exercise}, {@link ExerciseCourseLinkEntity#course}) exists.
     *
     * @return number of affected rows
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO exercise_course_link (exercise_id, course_id)
            VALUES (:exerciseId, :courseId)
            """,
            nativeQuery = true)
    int createIfAbsent(@Param("exerciseId") long exerciseId, @Param("courseId") long courseId);
}

