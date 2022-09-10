package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Domain", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "version"}))
@TypeDef(name = "json", typeClass = JsonStringType.class)
public class DomainEntity {
    @Id
    @Column(name = "name")
    private String name;

    @Column(name = "shortName", nullable = false, unique = true)
    private String shortName;

    @Column(name = "version", nullable = false)
    private String version;

    @Type(type = "json")
    @Column(name = "options_json", columnDefinition = "json", nullable = false)
    private DomainOptionsEntity options;
}
