package org.vstu.compprehension.data.domain;

import org.jetbrains.annotations.NotNull;

public record DomainData(
        @NotNull String name,
        @NotNull String shortName,
        @NotNull String version,
        DomainOptionsData options) {
}
