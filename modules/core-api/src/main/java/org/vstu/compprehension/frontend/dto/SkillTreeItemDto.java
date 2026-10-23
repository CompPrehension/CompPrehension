package org.vstu.compprehension.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
@Jacksonized
public class SkillTreeItemDto {
    @NotNull
    private String name;
    @NotNull
    private String displayName;
    @NotNull
    private final SkillTreeItemDto[] childs;
    private boolean targetEnabled;

    public SkillTreeItemDto(@NotNull String name, @NotNull String displayName, boolean targetEnabled) {
        this.name = name;
        this.displayName = displayName;
        this.childs = new SkillTreeItemDto[0];
        this.targetEnabled = targetEnabled;
    }
}
