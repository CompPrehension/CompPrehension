package org.vstu.compprehension.dto;

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

    public SkillTreeItemDto(@NotNull String name, @NotNull String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.childs = new SkillTreeItemDto[0];
    }
}
