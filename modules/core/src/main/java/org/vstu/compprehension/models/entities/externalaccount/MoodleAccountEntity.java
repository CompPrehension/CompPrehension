package org.vstu.compprehension.models.entities.externalaccount;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "moodle_account")
@DiscriminatorValue("MOODLE")
@Getter
@Setter
@NoArgsConstructor
public class MoodleAccountEntity extends ExternalAccountEntity {
    @Column(name = "moodle_user_id", nullable = false)
    private Long moodleUserId;
}
