package org.vstu.compprehension.models.entities.course;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAccountId implements Serializable {
    private Long userId;
    private Long educationResourceId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExternalAccountId other)) return false;
        return Objects.equals(userId, other.userId)
                && Objects.equals(educationResourceId, other.educationResourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, educationResourceId);
    }
}
