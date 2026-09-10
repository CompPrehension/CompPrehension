package org.vstu.compprehension.entities.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.entities.UserEntity;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "role_user_assignment")
public class RoleUserAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_scope_id", nullable = false)
    private PermissionScopeEntity permissionScope;

    public RoleUserAssignmentEntity(UserEntity user, RoleEntity role, PermissionScopeEntity scope) {
        this.user = user;
        this.role = role;
        this.permissionScope = scope;
    }
}
