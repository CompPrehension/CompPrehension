package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Jacksonized
public class LawDto {
    @Getter @NotNull
    private String name;
    @Getter @NotNull
    private String displayName;
    @Getter
    private int bitflags;
}
