package org.vstu.compprehension.utils.transactions;

import lombok.SneakyThrows;

import java.util.concurrent.Callable;

public class TransactionScopeFactoryStub implements TransactionScopeFactory {
    private static final TransactionScopeStub STUB = new TransactionScopeStub();

    @Override
    public TransactionScope create() {
        return STUB;
    }

    @Override
    public TransactionScope create(TransactionScope.PropagationBehavior propagation) {
        return STUB;
    }

    private static class TransactionScopeStub implements TransactionScope {
        @Override
        public PropagationBehavior getPropagationBehavior() {
            return PropagationBehavior.REQUIRED;
        }

        @Override
        public IsolationLevel getIsolationLevel() {
            return IsolationLevel.READ_COMMITTED;
        }

        @SneakyThrows
        @Override
        public <T> T execute(Callable<T> action) {
            return action.call();
        }

        @Override
        public void executeNoResult(Runnable action) {
            action.run();
        }
    }
}
