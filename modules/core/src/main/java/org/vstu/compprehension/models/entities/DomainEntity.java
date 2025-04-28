package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
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
    private DomainOptionsEntity options;

    @Column(name = "bkt_roster", columnDefinition = "TEXT")
    private String bktRoster;
}
