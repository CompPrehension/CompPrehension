package org.vstu.compprehension.models.entities.role;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.UserEntity;

@Entity
@Data
@NoArgsConstructor
@Table(name = "RoleUserAssignment")
public class RoleUserAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne
    @JoinColumn(name = "permission_scope_id", nullable = false)
    private PermissionScopeEntity permissionScope;
}
