package org.vstu.compprehension.models.entities.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.models.entities.UserEntity;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "external_account")
public class ExternalAccountEntity {
    @EmbeddedId
    private ExternalAccountId id = new ExternalAccountId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        foreignKey = @ForeignKey(name = "fk_external_account_user")
    )
    private UserEntity user;

    @MapsId("educationResourceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "education_resource_id",
        foreignKey = @ForeignKey(name = "fk_external_account_education_resource")
    )
    private EducationResourceEntity educationResource;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    public ExternalAccountEntity(UserEntity user, EducationResourceEntity educationResource, String externalId) {
        this.user = user;
        this.educationResource = educationResource;
        this.externalId = externalId;
    }
}
