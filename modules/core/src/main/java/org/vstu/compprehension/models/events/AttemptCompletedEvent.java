package org.vstu.compprehension.models.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a user successfully completes an exercise attempt (Decision.FINISH).
 * Used to trigger async grade passback to Moodle via LTI AGS.
 */
@Getter
public class AttemptCompletedEvent extends ApplicationEvent {
    private final Long attemptId;

    public AttemptCompletedEvent(Object source, Long attemptId) {
        super(source);
        this.attemptId = attemptId;
    }
}
