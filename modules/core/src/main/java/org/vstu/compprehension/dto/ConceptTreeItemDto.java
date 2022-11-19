package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Jacksonized
public class ConceptTreeItemDto {
    @Getter @NotNull
    private final String name;
    @Getter @NotNull
    private final String displayName;
    @Getter
    private final int bitflags;
    @Getter @NotNull
    private final ConceptTreeItemDto[] childs;

    public ConceptTreeItemDto(@NotNull String name, @NotNull String displayName, int bitflags) {
        this.name = name;
        this.displayName = displayName;
        this.bitflags = bitflags;
        this.childs = new ConceptTreeItemDto[0];
    }
}
