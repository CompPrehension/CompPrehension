package org.vstu.compprehension.entities.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.vstu.compprehension.enums.OutboxEventStatus;

@Converter(autoApply = true)
public class OutboxEventStatusConverter implements AttributeConverter<OutboxEventStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(OutboxEventStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public OutboxEventStatus convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        for (var status : OutboxEventStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown outbox event status code " + code);
    }
}
