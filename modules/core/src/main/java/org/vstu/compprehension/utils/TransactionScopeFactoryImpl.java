package org.vstu.compprehension.utils;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Singleton;

@Component
@Singleton
public class TransactionScopeFactoryImpl implements TransactionScopeFactory {
    private final PlatformTransactionManager txManager;

    public TransactionScopeFactoryImpl(PlatformTransactionManager txManager) {
        this.txManager = txManager;
    }

    @Override
    public TransactionScope create() {
        return new TransactionScopeImpl(txManager, TransactionScope.PropagationBehavior.REQUIRED, TransactionScope.IsolationLevel.READ_COMMITTED);
    }

    @Override
    public TransactionScope create(TransactionScope.PropagationBehavior propagationBehavior) {
        return new TransactionScopeImpl(txManager, propagationBehavior, TransactionScope.IsolationLevel.READ_COMMITTED);
    }
}
