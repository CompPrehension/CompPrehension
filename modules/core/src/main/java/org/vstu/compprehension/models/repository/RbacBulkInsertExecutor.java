package org.vstu.compprehension.models.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Пакетная вставка RBAC-строк (назначения ролей и scope доступа) с пропуском дубликатов:
 * строки, нарушающие unique-констрейнт, молча игнорируются через insert ignore,
 * поэтому конкурентная транзакция не падает.
 */
@Component
@RequiredArgsConstructor
public class RbacBulkInsertExecutor {
    private final JdbcTemplate jdbc;

    /**
     * Вставляет назначения ролей одним батчем. Дубли по {@code ux_rua_user_role_scope}
     * (уже существующие назначения) пропускаются.
     */
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

    /**
     * Вставляет scope доступа одним батчем. Дубли по {@code ux_permission_scope_kind_course_eduRes}
     * (уже существующие scope) пропускаются.
     */
    public void insertPermissionScopesIgnoringDuplicates(Collection<PermissionScopeRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Object[]> batch = rows.stream()
                .map(r -> new Object[]{r.kind(), r.courseId(), r.educationResourceId()})
                .toList();
        jdbc.batchUpdate(
                "insert ignore into permission_scope (kind, course_id, education_resource_id) values (?, ?, ?)",
                batch
        );
    }

    public record RoleAssignmentRow(long userId, long roleId, long scopeId) {
    }

    public record PermissionScopeRow(String kind, Long courseId, Long educationResourceId) {
    }
}
