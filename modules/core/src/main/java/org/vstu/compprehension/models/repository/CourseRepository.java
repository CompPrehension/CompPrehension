package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.dto.course.CourseDto;
import org.vstu.compprehension.models.entities.course.CourseEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Optional<CourseEntity> findByExternalCourseIdAndEducationResourceId(String externalCourseId, Long educationResourceId);

    List<CourseEntity> findByEducationResourceIdAndExternalCourseIdIsNotNull(Long educationResourceId);

    @Query("""
            select c from CourseEntity c
            where c.educationResource.id in (
                select ea.educationResource.id from ExternalAccountEntity ea
                where ea.user.id = :userId
            )
            """)
    List<CourseEntity> findCoursesByUserId(@Param("userId") Long userId);

    @Query("""
            select distinct new org.vstu.compprehension.dto.course.CourseDto(
                c.id, c.name, c.educationResource.id, c.educationResource.url
            )
            from RoleUserAssignmentEntity rua
            join rua.permissionScope ps
            join ps.course c
            where rua.user.id = :userId
              and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.COURSE
            """)
    List<CourseDto> findCourseDtosByUserId(@Param("userId") Long userId);

    @Query("""
            select new org.vstu.compprehension.dto.course.CourseDto(
                c.id, c.name, c.educationResource.id, c.educationResource.url
            )
            from CourseEntity c
            """)
    List<CourseDto> findAllCourseDtos();

    /**
     * Inserts a row only if no row with the same ({@link CourseEntity#educationResource}, {@link CourseEntity#externalCourseId}) exists.
     *
     * @return number of affected rows
     */
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
