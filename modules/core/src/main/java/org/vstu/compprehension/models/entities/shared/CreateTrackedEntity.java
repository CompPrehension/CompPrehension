package org.vstu.compprehension.models.entities.shared;

import java.util.Date;

/**
 * Сущность с отслеживанием времени создания
 */
public interface CreateTrackedEntity {
    Date getCreatedAt();
    void setCreatedAt(Date createdAt);
}
