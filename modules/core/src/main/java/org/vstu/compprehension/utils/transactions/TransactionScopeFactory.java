package org.vstu.compprehension.utils.transactions;

public interface TransactionScopeFactory {
    TransactionScope create();
    TransactionScope create(TransactionScope.PropagationBehavior propagation);
}
