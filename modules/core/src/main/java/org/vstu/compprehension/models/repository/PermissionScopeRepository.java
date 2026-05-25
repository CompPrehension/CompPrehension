package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionScopeRepository extends JpaRepository<PermissionScopeEntity, Long> {
    Optional<PermissionScopeEntity> findByKindAndCourseIsNullAndEducationResourceIsNull(PermissionScopeKind kind);

    Optional<PermissionScopeEntity> findByKindAndCourse_Id(PermissionScopeKind kind, Long courseId);

    Optional<PermissionScopeEntity> findByKindAndEducationResource_Id(PermissionScopeKind kind, Long educationResourceId);

    @Query("""
        select ps from PermissionScopeEntity ps
        where ps.kind = :kind and ps.course.id in :courseIds
        """)
    List<PermissionScopeEntity> findByKindAndCourseIdIn(
        @Param("kind") PermissionScopeKind kind,
        @Param("courseIds") Collection<Long> courseIds);
}
