package org.vstu.compprehension.models.businesslogic.date;

import java.time.Instant;

public interface DateTimeProvider extends org.springframework.data.auditing.DateTimeProvider {
    Instant now();
    void setTime(Instant time);
}
