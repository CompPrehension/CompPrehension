package org.vstu.compprehension.outbox;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.data.outbox.OutboxEventType;
import org.vstu.compprehension.enums.OutboxEventStatus;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxStoredValuesTest {

    /** Имена типов событий хранятся в {@code event_type}: константы только добавляются. */
    @Test
    void eventTypeNamesAreOnlyAdded() {
        // Arrange.
        var stored = List.of(
                "ATTEMPT_FINISHED");

        // Act.
        var current = Arrays.stream(OutboxEventType.values()).map(Enum::name).toList();

        // Assert.
        assertEquals(stored, current,
                "outbox_event.event_type stores these names: add new ones to the list, never rename or remove");
    }

    /** Коды статусов хранятся в {@code status}: код константы не меняется. */
    @Test
    void statusCodesStayTheSame() {
        // Arrange.
        var stored = Map.of(
                "PENDING", 0,
                "PROCESSED", 1,
                "FAILED", 2);

        // Act.
        var current = new LinkedHashMap<String, Integer>();
        for (var status : OutboxEventStatus.values()) {
            current.put(status.name(), status.getCode());
        }

        // Assert.
        assertEquals(stored, current,
                "outbox_event.status stores these codes: add new statuses to the map, never change existing codes");
    }
}
