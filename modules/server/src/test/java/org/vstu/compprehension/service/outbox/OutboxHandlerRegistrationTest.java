package org.vstu.compprehension.service.outbox;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.vstu.compprehension.config.OutboxProperties;
import org.vstu.compprehension.data.outbox.AttemptFinishedEvent;
import org.vstu.compprehension.data.outbox.OutboxEvent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxHandlerRegistrationTest {

    static class GradeHandler implements OutboxEventHandler<AttemptFinishedEvent> {
        @Override
        public void handle(@NotNull AttemptFinishedEvent event) {
        }
    }

    static class AnotherGradeHandler implements OutboxEventHandler<AttemptFinishedEvent> {
        @Override
        public void handle(@NotNull AttemptFinishedEvent event) {
        }
    }

    record UntypedEvent() implements OutboxEvent {
    }

    static class UntypedEventHandler implements OutboxEventHandler<UntypedEvent> {
        @Override
        public void handle(@NotNull UntypedEvent event) {
        }
    }

    /** Два обработчика одного типа событий — ошибка запуска, а не случайный выбор одного из них. */
    @Test
    void twoHandlersForSameEventFailStartup() {
        // Act & Assert.
        assertThrows(IllegalStateException.class, () -> new OutboxProcessor(null, new OutboxProperties(),
                List.of(new GradeHandler(), new AnotherGradeHandler())));
    }

    /** Обработчик события без @EventType — ошибка запуска: такие события не публикуются. */
    @Test
    void handlerOfEventWithoutTypeFailsStartup() {
        // Act & Assert.
        assertThrows(IllegalStateException.class, () -> new OutboxProcessor(null, new OutboxProperties(),
                List.of(new UntypedEventHandler())));
    }

    /** У лямбды класс события не прочитать — ошибка запуска, а не молча пропущенные события. */
    @Test
    void handlerWithoutDeclaredEventClassFailsStartup() {
        // Arrange.
        OutboxEventHandler<AttemptFinishedEvent> lambdaHandler = event -> { };

        // Act & Assert.
        assertThrows(IllegalStateException.class, () -> new OutboxProcessor(null, new OutboxProperties(),
                List.of(lambdaHandler)));
    }
}
