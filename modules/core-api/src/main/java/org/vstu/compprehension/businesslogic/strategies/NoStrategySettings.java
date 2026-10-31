package org.vstu.compprehension.businesslogic.strategies;

import org.vstu.compprehension.businesslogic.settings.SettingsType;

/** Настройки стратегии, у которой нет собственных параметров. */
public record NoStrategySettings() {

    public static final SettingsType<NoStrategySettings> TYPE =
            new SettingsType<>(NoStrategySettings.class, new NoStrategySettings());
}
