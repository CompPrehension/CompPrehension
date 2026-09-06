package org.vstu.compprehension.utils.transactions;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Callable;

public class TransactionScopeImpl implements TransactionScope {
    private final TransactionTemplate transactionTemplate;
    private final PropagationBehavior propagationBehavior;
    private final IsolationLevel isolationLevel;

    public TransactionScopeImpl(PlatformTransactionManager txManager, PropagationBehavior propagationBehavior, IsolationLevel isolationLevel) {
        this.transactionTemplate = new TransactionTemplate(txManager);
        this.propagationBehavior = propagationBehavior;
        this.isolationLevel = isolationLevel;

        var preparedPropagation = switch (propagationBehavior) {
            case REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED;
            case REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            default -> throw new IllegalStateException("Unexpected value: " + propagationBehavior);
        };
        var preparedIsolationLevel = switch (isolationLevel) {
            case READ_COMMITTED -> TransactionDefinition.ISOLATION_READ_COMMITTED;
            default -> throw new IllegalStateException("Unexpected value: " + isolationLevel);
        };
        
        this.transactionTemplate.setPropagationBehavior(preparedPropagation);
        this.transactionTemplate.setIsolationLevel(preparedIsolationLevel);
    }

    @Override
    public PropagationBehavior getPropagationBehavior() {
        return propagationBehavior;
    }

    @Override
    public IsolationLevel getIsolationLevel() {
        return isolationLevel;
    }

    @Override
    public <T> T execute(Callable<T> action) {
        return transactionTemplate.execute(status -> {
            try {
                return action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void executeNoResult(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                action.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
