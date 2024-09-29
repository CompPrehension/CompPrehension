package org.vstu.compprehension.models.entities.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.auth.PermissionScopeKind;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "permission_scope",
       uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "related_object_id"}))
public class PermissionScopeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "parent_scope_id", nullable = true)
    @Nullable
    private Integer parentScopeId;

    @Column(name = "kind", nullable = false)
    @Enumerated(EnumType.STRING)
    private PermissionScopeKind kind;

    @Column(name = "related_object_id", nullable = true)
    @Nullable
    private Long relatedObjectId;
}
