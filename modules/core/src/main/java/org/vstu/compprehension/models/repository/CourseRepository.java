package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.CourseEntity;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Optional<CourseEntity> findByExternalCourseIdAndEducationResourceId(String externalCourseId, Long educationResourceId);

    /**
     * Inserts a row only if no row with the same ({@link CourseEntity#educationResource}, {@link CourseEntity#externalCourseId}) exists.
     *
     * @return number of affected rows
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO course (external_course_id, name, education_resource_id)
            VALUES (:externalCourseId, :name, :educationResourceId)
            """, nativeQuery = true)
    int createIfAbsent(
            @Param("externalCourseId") String externalCourseId,
            @Param("name") String name,
            @Param("educationResourceId") Long educationResourceId
    );

    /**
     * Inserts the entity only if it does not already exist.
     *
     * @return {@code true} if a new row was created, {@code false} if a row already existed
     */
    default boolean createIfAbsent(CourseEntity course) {
        if (course.getId() != null) {
            throw new IllegalArgumentException("Use save() to update existing CourseEntity");
        }
        return createIfAbsent(
                course.getExternalCourseId(),
                course.getName(),
                course.getEducationResource().getId()
        ) > 0;
    }
}
