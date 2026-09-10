package org.vstu.compprehension.entities.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.businesslogic.auth.Permission;
import org.vstu.compprehension.entities.converters.PermissionConverter;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "permission")
public class PermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PermissionConverter.class)
    @Column(name = "name", nullable = false, unique = true, length = 64, updatable = false)
    private Permission name;

    public PermissionEntity(Permission name) {
        this.name = name;
    }
}
