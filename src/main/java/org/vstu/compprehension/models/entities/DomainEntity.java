package org.vstu.compprehension.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Domain", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "version"}))
public class DomainEntity {
    @Id
    @Column(name = "name")
    private String name;

    @Column(name = "classPath", length = 1000, nullable = false)
    private String classPath;

    @Column(name = "version", nullable = false)
    private String version;
}
