package org.vstu.compprehension.adapters;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.Collection;
import java.util.List;

public class RateLimitBackendDecorator implements Backend {
    private final @NotNull String backendId;
    private final @NotNull Backend decoratee;
    private final @NotNull BackendTaskQueue taskQueue;

    public RateLimitBackendDecorator(@NotNull String backendId,
                                     @NotNull Backend decoratee,
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

    @SneakyThrows
    @Override
    public Collection<Fact> solve(List<Law> laws, List<BackendFactEntity> statement, ReasoningOptions reasoningOptions) {
        return taskQueue.postAsync(() -> decoratee.solve(laws, statement, reasoningOptions)).get();
    }

    @SneakyThrows
    @Override
    public Collection<Fact> solve(List<Law> laws, Collection<Fact> statement, ReasoningOptions reasoningOptions) {
        return taskQueue.postAsync(() -> decoratee.solve(laws, statement, reasoningOptions)).get();
    }

    @SneakyThrows
    @Override
    public Collection<Fact> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, ReasoningOptions reasoningOptions) {
        return taskQueue.postAsync(() -> decoratee.judge(laws, statement, correctAnswer, response, reasoningOptions)).get();
    }

    @SneakyThrows
    @Override
    public Collection<Fact> judge(List<Law> laws, Collection<Fact> statement, Collection<Fact> correctAnswer, Collection<Fact> response, ReasoningOptions reasoningOptions) {
        return taskQueue.postAsync(() -> decoratee.judge(laws, statement, correctAnswer, response, reasoningOptions)).get();
    }
}
