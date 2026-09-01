package org.vstu.compprehension.models.entities.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.models.entities.UserEntity;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "role_user_assignment")
public class RoleUserAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permission_scope_id", nullable = false)
    private PermissionScopeEntity permissionScope;

    public RoleUserAssignmentEntity(UserEntity user, RoleEntity role, PermissionScopeEntity scope) {
        this.user = user;
        this.role = role;
        this.permissionScope = scope;
    }
}
