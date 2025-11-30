package org.vstu.compprehension.utils;

import java.util.concurrent.Callable;

public interface TransactionScope {
    enum PropagationBehavior {
        REQUIRED,
        REQUIRES_NEW,
    }
    
    enum IsolationLevel {
        READ_COMMITTED,
    }

    PropagationBehavior getPropagationBehavior();
    IsolationLevel getIsolationLevel();
    <T> T execute(Callable<T> action);
    void executeNoResult(Runnable action);
}
