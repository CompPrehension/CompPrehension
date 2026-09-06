package org.vstu.compprehension.adapters;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.backend.Backend;

public class RateLimitBackendDecorator<I, O> implements Backend<I, O> {
    private final @NotNull String backendId;
    private final @NotNull Backend<I, O> decoratee;
    private final @NotNull BackendTaskQueue taskQueue;

    public RateLimitBackendDecorator(@NotNull String backendId,
                                     @NotNull Backend<I, O> decoratee,
                                     @NotNull BackendTaskQueue taskQueue) {
        this.backendId = backendId;
        this.decoratee = decoratee;
        this.taskQueue = taskQueue;
    }

    @NotNull
    @Override
    public String getBackendId() {
        return backendId;
    }

    @Override
    public Backend<I, O> getActualBackend() {
        return decoratee.getActualBackend();
    }

    @SneakyThrows
    @Override
    public O judge(I questionData) {
        return taskQueue.postAsync(() -> decoratee.judge(questionData)).get();
    }

    @SneakyThrows
    @Override
    public O solve(I questionData) {
        return taskQueue.postAsync(() -> decoratee.solve(questionData)).get();
    }
}
