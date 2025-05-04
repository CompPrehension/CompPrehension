package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(BktDataEntity.BktDataId.class)
@Table(name = "bkt_data", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "domain_name"}))
public class BktDataEntity {

    @Getter @Setter
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BktDataId implements Serializable {
        private Long user;
        private String domain;
    }

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private @NotNull UserEntity user;

    @Id
    @ManyToOne
    @JoinColumn(name = "domain_name", nullable = false)
    private @NotNull DomainEntity domain;

    @Version
    @Column(name = "version", columnDefinition = "BIGINT", nullable = false)
    private long version;

    @Column(name = "roster", columnDefinition = "TEXT", nullable = false)
    private @NotNull String roster;
}
