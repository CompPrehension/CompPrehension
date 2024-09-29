package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.vstu.compprehension.models.entities.auth.PermissionScopeEntity;
import org.vstu.compprehension.models.entities.auth.RoleEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "user_role_assignment")
@IdClass(UserRoleAssignmentEntity.UserRoleAssignmentKey.class)
public class UserRoleAssignmentEntity {
    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Id
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Id
    @ManyToOne
    @JoinColumn(name = "permission_scope_id", nullable = false)
    private PermissionScopeEntity permissionScope;

    @Getter @Setter
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class UserRoleAssignmentKey implements Serializable {
        private long user;
        private long role;
        private int permissionScope;
    }
}
