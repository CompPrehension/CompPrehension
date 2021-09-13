package org.vstu.compprehension.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.vstu.compprehension.utils.threads.ContextAwarePoolExecutor;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Executes action in thread pool or put it into the queue if all threads are busy
 */
@Log4j2
public class TaskQueue {
    private final ExecutorService threadPool;

    public TaskQueue(int concurrencyLevel, int maxQueueSize) {
        this.threadPool = new ContextAwarePoolExecutor(concurrencyLevel, concurrencyLevel,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(maxQueueSize));
    }

    @Async
    public <R>Future<R> postAsync(Supplier<R> action) {
        log.debug("Async action submitted. Worker: {}", this.getClass().getName());
        return threadPool.submit(action::get);
    }

    public void shutdown() {
        log.debug("Async worker shutdown. Worker: {}", this.getClass().getName());
        threadPool.shutdown();
    }
}
