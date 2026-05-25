package org.vstu.compprehension.models.entities.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.entities.converters.RoleConverter;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "role")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = RoleConverter.class)
    @Column(name = "name", nullable = false, unique = true, length = 64, updatable = false)
    private Role name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permission",
        joinColumns = @JoinColumn(name = "role_id", updatable = false),
        inverseJoinColumns = @JoinColumn(name = "permission_id", updatable = false)
    )
    private List<PermissionEntity> permissions;

    public RoleEntity(Role name, List<PermissionEntity> permissions) {
        this.name = name;
        this.permissions = permissions;
    }
}
