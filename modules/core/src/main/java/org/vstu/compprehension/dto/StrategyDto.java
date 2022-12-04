package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Value;
import org.vstu.compprehension.models.businesslogic.strategies.StrategyOptions;

@Value @Builder
public class StrategyDto {
    String id;
    StrategyOptions options;
}
