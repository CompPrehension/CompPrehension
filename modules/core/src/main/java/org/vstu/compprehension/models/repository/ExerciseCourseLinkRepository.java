package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkId;

import java.util.Collection;
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

    /**
     * Связи курса с перечисленными упражнениями, с уже загруженным упражнением.
     * <p>
     * join fetch здесь обязателен: {@link ExerciseCourseLinkEntity#getExercise()} — ленивая
     * связь, и без него вызывающий код получит прокси, который вне транзакции не
     * инициализируется.
     */
    @Query("""
            select ecl from ExerciseCourseLinkEntity ecl
            join fetch ecl.exercise
            where ecl.course.id = :courseId and ecl.exercise.id in :exerciseIds
            """)
    List<ExerciseCourseLinkEntity> findAllByCourseIdAndExerciseIdsFetchingExercise(
            @Param("courseId") long courseId,
            @Param("exerciseIds") Collection<Long> exerciseIds);

    @Query("""
            select ecl.course.id as id, ecl.course.name as name,
                   ecl.course.educationResource.id as educationResourceId,
                   ecl.course.educationResource.url as educationResourceUrl
            from ExerciseCourseLinkEntity ecl
            where ecl.exercise.id = :exerciseId
            """)
    List<CourseRepository.CourseView> findCourseViewsByExerciseId(@Param("exerciseId") long exerciseId);

    @Modifying(clearAutomatically = true)
    void deleteByExerciseIdAndCourseId(long exerciseId, long courseId);

    @Modifying(clearAutomatically = true)
    void deleteByExerciseId(long exerciseId);
}

