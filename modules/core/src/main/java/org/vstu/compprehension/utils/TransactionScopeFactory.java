package org.vstu.compprehension.utils;

public interface TransactionScopeFactory {
    TransactionScope create();
    TransactionScope create(TransactionScope.PropagationBehavior propagation);
}
