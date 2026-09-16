package org.vstu.compprehension.frontend.dto;

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
    private boolean targetEnabled;
    @NotNull
    private final LawTreeItemDto[] childs;

    public LawTreeItemDto(@NotNull String name, @NotNull String displayName, boolean targetEnabled) {
        this.name = name;
        this.displayName = displayName;
        this.targetEnabled = targetEnabled;
        this.childs = new LawTreeItemDto[0];
    }
}
