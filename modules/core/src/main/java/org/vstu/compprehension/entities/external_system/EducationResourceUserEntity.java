package org.vstu.compprehension.entities.external_system;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.entities.UserEntity;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "education_resource_user")
public class EducationResourceUserEntity {
    @EmbeddedId
    private EducationResourceUserId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @MapsId("educationResourceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "education_resource_id")
    private EducationResourceEntity educationResource;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "full_name")
    private @Nullable String fullName;

    @Column(name = "email")
    private @Nullable String email;

    public EducationResourceUserEntity(@NotNull UserEntity user, @NotNull EducationResourceEntity educationResource,
                                       @NotNull String externalId) {
        this.id = new EducationResourceUserId(user.getId(), educationResource.getId());
        this.user = user;
        this.educationResource = educationResource;
        this.externalId = externalId;
    }
}
