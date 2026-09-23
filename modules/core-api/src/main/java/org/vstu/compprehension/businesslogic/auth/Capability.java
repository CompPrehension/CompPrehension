package org.vstu.compprehension.businesslogic.auth;

/**
 * Способность пользователя: назначается ролью в любой области, проверяется без области.
 */
public interface Capability {

    /** Значение колонки {@code permission.name}. */
    String id();
}
