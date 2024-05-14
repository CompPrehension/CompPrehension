package org.vstu.compprehension.models.entities.role;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "PermissionScope")
public class PermissionScopeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "kind")
    private String kind;

    @Column(name = "owner_id")
    private Long ownerId;

    @OneToMany(mappedBy = "permissionScope")
    private List<RoleUserAssignmentEntity> roleUserAssignments;
}
