package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface PermissionScopeRepository extends JpaRepository<PermissionScopeEntity, Long> {

    @Query("""
        select ps from PermissionScopeEntity ps
        where ps.kind = :kind and ps.course.id in :courseIds
        """)
    List<PermissionScopeEntity> findByKindAndCourseIdIn(
            @Param("kind") PermissionScopeKind kind,
            @Param("courseIds") Collection<Long> courseIds
    );

    @Modifying
    @Query(value = """
        insert ignore into permission_scope (kind, course_id, education_resource_id)
        values (:kind, :courseId, :eduResId)
        """, nativeQuery = true)
    int createIfAbsent(@Param("kind") String kind, @Param("courseId") Long courseId, @Param("eduResId") Long eduResId);
}
