package org.vstu.compprehension.utils.threads;

import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.*;

public class ContextAwarePoolTaskExecutor extends ThreadPoolTaskExecutor {

    @Override
    public @NotNull <T> Future<T> submit(@NotNull Callable<T> task) {
        return super.submit(new ContextAwareCallable<T>(task, RequestContextHolder.currentRequestAttributes(), ThreadContext.getContext()));
    }

    @Deprecated
    @Override
    public @NotNull <T> ListenableFuture<T> submitListenable(@NotNull Callable<T> task) {
        return super.submitListenable(new ContextAwareCallable<T>(task, RequestContextHolder.currentRequestAttributes(), ThreadContext.getContext()));
    }

    @Override
    public @NotNull <T> CompletableFuture<T> submitCompletable(@NotNull Callable<T> task) {
        return super.submitCompletable(new ContextAwareCallable<T>(task, RequestContextHolder.currentRequestAttributes(), ThreadContext.getContext()));
    }
}
