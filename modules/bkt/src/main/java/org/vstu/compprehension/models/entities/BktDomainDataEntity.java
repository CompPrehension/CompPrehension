package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bkt_domain_data")
public class BktDomainDataEntity {

    @Id
    @Column(name = "domain_name")
    private @NotNull String domainName;

    @Column(name = "empty_roster", columnDefinition = "TEXT", nullable = false)
    private @NotNull String emptyRoster;
}
