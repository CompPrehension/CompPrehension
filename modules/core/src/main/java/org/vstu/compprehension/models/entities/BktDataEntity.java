package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bkt_data")
public class BktDataEntity {

    @Id
    @Column(name = "domain_name", nullable = false)
    private String domainName;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_name", nullable = false)
    private @NotNull DomainEntity domain;

    @Version
    @Column(name = "version", columnDefinition = "BIGINT", nullable = false)
    private long version;

    @Column(name = "roster", columnDefinition = "TEXT", nullable = false)
    private @NotNull String roster;
}
