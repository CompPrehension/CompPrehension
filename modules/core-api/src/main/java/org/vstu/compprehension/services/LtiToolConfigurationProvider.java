package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.lti.LtiToolConfigurationData;

/**
 * Out-port: CompPrehension как LTI-инструмент.
 */
public interface LtiToolConfigurationProvider {
    /** @throws IllegalStateException если адрес или ключ инструмента не заданы */
    @NotNull LtiToolConfigurationData getToolConfiguration();
}
