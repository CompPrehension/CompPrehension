package org.vstu.compprehension.models.entities.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "permission_scope")
public class PermissionScopeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private PermissionScopeKind kind;

    @Column(name = "scope_item_id")
    private Long scopeItemId;

    public static PermissionScopeEntity ofGlobalScope() {
        var scope = new PermissionScopeEntity();
        scope.kind = PermissionScopeKind.GLOBAL;
        return scope;
    }

    public static PermissionScopeEntity ofCourseScope(Long courseId) {
        var scope = new PermissionScopeEntity();
        scope.kind = PermissionScopeKind.COURSE;
        scope.scopeItemId = courseId;
        return scope;
    }

    public static PermissionScopeEntity ofEducationResourceScope(Long educationResourceId) {
        var scope = new PermissionScopeEntity();
        scope.kind = PermissionScopeKind.EDUCATION_RESOURCE;
        scope.scopeItemId = educationResourceId;
        return scope;
    }
}
