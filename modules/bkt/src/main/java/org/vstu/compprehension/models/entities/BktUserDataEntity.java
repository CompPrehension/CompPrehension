package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(BktUserDataEntity.BktUserDataId.class)
@Table(name = "bkt_user_data", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "domain_name"}))
public class BktUserDataEntity {

    @Getter @Setter
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BktUserDataId implements Serializable {
        private Long userId;
        private String domainName;
    }

    @Id
    @Column(name = "user_id")
    private @NotNull Long userId;

    @Id
    @Column(name = "domain_name")
    private @NotNull String domainName;

    @Version
    @Column(name = "version", columnDefinition = "BIGINT", nullable = false)
    private long version;

    @Column(name = "roster", columnDefinition = "TEXT", nullable = false)
    private @NotNull String roster;
}
