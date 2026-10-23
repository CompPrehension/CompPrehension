package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.outbox.ClaimedOutboxEventData;
import org.vstu.compprehension.data.outbox.OutboxEventType;
import org.vstu.compprehension.entities.OutboxEventEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;

@Component
class ClaimedOutboxEventMapper implements Mapper<OutboxEventEntity, ClaimedOutboxEventData> {

    @Override
    public @NotNull ClaimedOutboxEventData map(@NotNull OutboxEventEntity source) {
        long id = Strict.required(source.getId(), "id", "outbox event");
        String owner = "outbox event " + id;
        return new ClaimedOutboxEventData(
                id,
                OutboxEventType.valueOf(Strict.required(source.getEventType(), "eventType", owner)),
                source.getAttempts(),
                Strict.required(source.getPayload(), "payload", owner));
    }
}
