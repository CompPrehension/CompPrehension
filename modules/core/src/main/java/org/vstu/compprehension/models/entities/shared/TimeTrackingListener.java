package org.vstu.compprehension.models.entities.shared;

import jakarta.persistence.PrePersist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.date.DateTimeProvider;

import java.util.Date;

@Component
public class TimeTrackingListener {

    private static DateTimeProvider currentDateProvider;

    @Autowired
    public void init(DateTimeProvider provider) {
        // В JPA Listeners нельзя инжектить бины напрямую через конструктор,
        // поэтому используем статический сеттер
        TimeTrackingListener.currentDateProvider = provider;
    }

    @PrePersist
    public void setCreatedAt(Object entity) {
        if (entity instanceof CreateTrackedEntity tracked) {
            // заполняем только если не заполнено
            if (tracked.getCreatedAt() == null) {
                Date now = Date.from(currentDateProvider.now());
                tracked.setCreatedAt(now);
            }
        }
    }
}
