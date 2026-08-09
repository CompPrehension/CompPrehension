package org.vstu.compprehension.models.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RbacBulkInsertExecutor {
    private final JdbcTemplate jdbc;

    public void insertRoleAssignmentsIgnoringDuplicates(Collection<RoleAssignmentRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Object[]> batch = rows.stream()
                .map(r -> new Object[]{r.userId(), r.roleId(), r.scopeId()})
                .toList();
        jdbc.batchUpdate(
                "insert ignore into role_user_assignment (user_id, role_id, permission_scope_id) values (?, ?, ?)",
                batch
        );
    }

    public void insertPermissionScopesIgnoringDuplicates(Collection<PermissionScopeRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Object[]> batch = rows.stream()
                .map(r -> new Object[]{r.kind(), r.scopeItemId()})
                .toList();
        jdbc.batchUpdate(
                "insert ignore into permission_scope (kind, scope_item_id) values (?, ?)",
                batch
        );
    }

    public record RoleAssignmentRow(long userId, long roleId, long scopeId) {
    }

    public record PermissionScopeRow(String kind, Long scopeItemId) {
    }
}
