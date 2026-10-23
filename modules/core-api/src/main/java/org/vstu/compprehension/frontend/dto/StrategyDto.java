package org.vstu.compprehension.frontend.dto;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.strategies.StrategyOptions;

@Value @Builder
public class StrategyDto {
    @NotNull String id;
    @NotNull String displayName;
    @Nullable String description;
    @NotNull StrategyOptions options;
}
