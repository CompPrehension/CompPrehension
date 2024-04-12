package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
@Jacksonized
public class LawTreeItemDto {
    @NotNull
    private String name;
    @NotNull
    private String displayName;
    private int bitflags;
    @NotNull
    private final LawTreeItemDto[] childs;

    public LawTreeItemDto(@NotNull String name, @NotNull String displayName, int bitflags) {
        this.name = name;
        this.displayName = displayName;
        this.bitflags = bitflags;
        this.childs = new LawTreeItemDto[0];
    }
}
