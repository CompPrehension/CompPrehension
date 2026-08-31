package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.dto.course.CourseDto;
import org.vstu.compprehension.models.entities.course.CourseEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Optional<CourseEntity> findByExternalCourseIdAndEducationResourceId(String externalCourseId, Long educationResourceId);

    List<CourseEntity> findByEducationResourceIdAndExternalCourseIdIsNotNull(Long educationResourceId);

    @Query("""
            select new org.vstu.compprehension.dto.course.CourseDto(
                c.id, c.name, c.educationResource.id, c.educationResource.url
            )
            from CourseEntity c
            where c.id in :courseIds
            """)
    List<CourseDto> findCourseDtosByIdIn(@Param("courseIds") Collection<Long> courseIds);

    @Query("""
            select new org.vstu.compprehension.dto.course.CourseDto(
                c.id, c.name, c.educationResource.id, c.educationResource.url
            )
            from CourseEntity c
            """)
    List<CourseDto> findAllCourseDtos();

    @Query("select c.id from CourseEntity c where c.educationResource.id in :educationResourceIds")
    List<Long> findCourseIdsByEducationResourceIdIn(@Param("educationResourceIds") Collection<Long> educationResourceIds);

    @Query("select c.educationResource.id from CourseEntity c where c.id = :courseId")
    Optional<Long> findEducationResourceIdByCourseId(@Param("courseId") Long courseId);

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
