package org.vstu.compprehension.models.businesslogic.date;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

@Service
public class CurrentDateTimeProvider implements DateTimeProvider {
    private Instant virtualTime = null; // Если null — работаем в реальном времени

    /**
     * Установить виртуальное время
     */
    public void setTime(Instant time) {
        this.virtualTime = time;
    }

    /**
     * Сброс виртуального времени
     */
    public void reset() {
        this.virtualTime = null;
    }

    /**
     * Текущее время
     */
    public Instant now() {
        return virtualTime != null ? virtualTime : Instant.now();
    }

    @NotNull
    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(now());
    }
}
