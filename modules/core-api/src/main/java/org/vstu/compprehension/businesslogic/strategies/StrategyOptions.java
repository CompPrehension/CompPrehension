package org.vstu.compprehension.businesslogic.strategies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Getter
@AllArgsConstructor @NoArgsConstructor
@Builder
@Jacksonized
public class StrategyOptions {
    private boolean multiStagesEnabled;
    private boolean visibleToUser = false;
}
