package org.vstu.compprehension.enums;

import lombok.Getter;

/**
 * Статус события очереди.
 */
@Getter
public enum OutboxEventStatus {
    PENDING(0),
    PROCESSED(1),
    FAILED(2);

    private final int code;

    OutboxEventStatus(int code) {
        this.code = code;
    }
}
