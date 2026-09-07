package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.course.CourseEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    interface CourseView {
        long getId();
        String getExternalCourseId();
        String getName();
        long getEducationResourceId();
        String getEducationResourceUrl();
    }

    Optional<CourseEntity> findByExternalCourseIdAndEducationResourceId(String externalCourseId, Long educationResourceId);

    @Query("""
            select c.id as id, c.name as name, c.externalCourseId as externalCourseId, c.educationResource.id as educationResourceId, c.educationResource.url as educationResourceUrl
            from CourseEntity c
            where c.educationResource.id = :educationResourceId and c.externalCourseId is not null
            """)
    List<CourseView> findExternalCourses(@Param("educationResourceId") long educationResourceId);

    @Modifying(clearAutomatically = true)
    @Query("update CourseEntity c set c.externalCourseId = null where c.id in :courseIds")
    int detachFromExternalSystem(@Param("courseIds") Collection<Long> courseIds);

    @Query("""
            select c.id as id, c.name as name,
                   c.educationResource.id as educationResourceId,
                   c.educationResource.url as educationResourceUrl
            from CourseEntity c
            where c.id in :courseIds
            """)
    List<CourseView> findCourseViewsByIdIn(@Param("courseIds") Collection<Long> courseIds);

    @Query("""
            select c.id as id, c.name as name,
                   c.educationResource.id as educationResourceId,
                   c.educationResource.url as educationResourceUrl
            from CourseEntity c
            """)
    List<CourseView> findAllCourseViews();

    @Query("select c.id from CourseEntity c where c.educationResource.id in :educationResourceIds")
    List<Long> findCourseIdsByEducationResourceIdIn(@Param("educationResourceIds") Collection<Long> educationResourceIds);

    @Query("select c.educationResource.id from CourseEntity c where c.id = :courseId")
    Optional<Long> findEducationResourceIdByCourseId(@Param("courseId") Long courseId);

    @Query("""
            select c.id as id, c.name as name, c.externalCourseId as externalCourseId, c.educationResource.id as educationResourceId, c.educationResource.url as educationResourceUrl
            from CourseEntity c
            where c.id in :courseIds
            """)
    List<CourseView> findEducationResourceRefsByCourseIdIn(@Param("courseIds") Collection<Long> courseIds);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            insert ignore into course (external_course_id, name, education_resource_id)
            value(:externalCourseId, :name, :educationResourceId)
            """, nativeQuery = true)
    int createIfAbsent(
            @Param("externalCourseId") String externalCourseId,
            @Param("name") String name,
            @Param("educationResourceId") Long educationResourceId
    );
}
