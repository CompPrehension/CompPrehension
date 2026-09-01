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
        where ps.kind = :kind and ps.scopeItemId in :scopeItemIds
        """)
    List<PermissionScopeEntity> findByKindAndScopeItemIdIn(
            @Param("kind") PermissionScopeKind kind,
            @Param("scopeItemIds") Collection<Long> scopeItemIds
    );

    @Modifying
    @Query(value = """
        insert ignore into permission_scope (kind, scope_item_id)
        values (:kind, :scopeItemId)
        """, nativeQuery = true)
    int createIfAbsent(@Param("kind") String kind, @Param("scopeItemId") Long scopeItemId);
}
