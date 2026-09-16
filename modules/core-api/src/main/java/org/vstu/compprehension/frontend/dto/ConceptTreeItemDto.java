package org.vstu.compprehension.frontend.dto;

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
    private final boolean targetEnabled;
    @Getter @NotNull
    private final ConceptTreeItemDto[] childs;

    public ConceptTreeItemDto(@NotNull String name, @NotNull String displayName, boolean targetEnabled) {
        this.name = name;
        this.displayName = displayName;
        this.targetEnabled = targetEnabled;
        this.childs = new ConceptTreeItemDto[0];
    }
}
