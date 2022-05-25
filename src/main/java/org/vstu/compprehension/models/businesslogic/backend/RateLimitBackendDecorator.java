package org.vstu.compprehension.models.businesslogic.backend;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.entities.BackendFactEntity;

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
    public List<BackendFactEntity> solve(List<Law> laws, List<BackendFactEntity> statement, List<String> solutionVerbs) {
        return taskQueue.postAsync(() -> decoratee.solve(laws, statement, solutionVerbs)).get();
    }

    @SneakyThrows
    @Override
    public List<BackendFactEntity> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, List<String> violationVerbs) {
        return taskQueue.postAsync(() -> decoratee.judge(laws, statement, correctAnswer, response, violationVerbs)).get();
    }
}
