package org.vstu.compprehension.models.entities.shared;

import jakarta.persistence.EntityListeners;
import lombok.Builder;

@EntityListeners(TimeTrackingListener.class)
public abstract class BaseEntity {
}
