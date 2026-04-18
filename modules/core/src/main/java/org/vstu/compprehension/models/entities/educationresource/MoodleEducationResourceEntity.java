package org.vstu.compprehension.models.entities.educationresource;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "moodle_education_resource")
@DiscriminatorValue("MOODLE")
@Getter
@Setter
@NoArgsConstructor
public class MoodleEducationResourceEntity extends EducationResourceEntity {
}
