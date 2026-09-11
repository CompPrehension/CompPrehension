package org.vstu.compprehension.entities;

import org.vstu.compprehension.data.domain.DomainOptionsData;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Domain", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "version"}))
public class DomainEntity {
    @Id
    @Column(name = "name")
    private String name;

    @Column(name = "shortName", nullable = false, unique = true)
    private String shortName;

    @Column(name = "version", nullable = false)
    private String version;

    @Type(JsonType.class)
    @Column(name = "options_json", columnDefinition = "json", nullable = false)
    private DomainOptionsData options;
}
