package org.vstu.compprehension.models.entities.role;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "PermissionScope", uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "owner_id"}))
public class PermissionScopeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "kind", nullable = false)
    @Enumerated(EnumType.STRING)
    private PermissionScopeKind kind;

    @Column(name = "owner_id")
    private Long ownerId;

    @OneToMany(mappedBy = "permissionScope")
    private List<RoleUserAssignmentEntity> roleUserAssignments;
}
