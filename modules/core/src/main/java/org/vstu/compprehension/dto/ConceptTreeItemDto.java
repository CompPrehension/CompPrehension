package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@AllArgsConstructor
@Jacksonized
public class ConceptTreeItemDto {
    @Getter @NotNull
    private final String name;
    @Getter
    private final int bitflags;
    @Getter @NotNull
    private final ConceptTreeItemDto[] childs;

    public ConceptTreeItemDto(String name, int bitflags) {
        this.name = name;
        this.bitflags = bitflags;
        this.childs = new ConceptTreeItemDto[0];
    }
}
