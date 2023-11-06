package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Value @Builder
public class DomainDto {
    @NotNull String id;
    @NotNull String displayName;
    @Nullable String description;
    @NotNull List<LawDto> laws;
    @NotNull List<ConceptTreeItemDto> concepts;
    @NotNull List<String> tags;
}
