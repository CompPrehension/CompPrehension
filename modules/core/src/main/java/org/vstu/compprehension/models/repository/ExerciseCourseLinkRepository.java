package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.dto.course.CourseDto;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkId;

import java.util.List;

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

    List<ExerciseCourseLinkEntity> findAllByExerciseId(long exerciseId);

    @Query("""
            select new org.vstu.compprehension.dto.course.CourseDto(
                ecl.course.id, ecl.course.name,
                ecl.course.educationResource.id, ecl.course.educationResource.url
            )
            from ExerciseCourseLinkEntity ecl
            where ecl.exercise.id = :exerciseId
            """)
    List<CourseDto> findCourseDtosByExerciseId(@Param("exerciseId") long exerciseId);

    @Modifying(clearAutomatically = true)
    void deleteByExerciseIdAndCourseId(long exerciseId, long courseId);
}

