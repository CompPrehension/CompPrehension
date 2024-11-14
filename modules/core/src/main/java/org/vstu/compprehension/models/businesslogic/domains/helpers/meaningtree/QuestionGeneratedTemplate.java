package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AllArgsConstructor
public class QuestionGeneratedTemplate {
    @Getter
    @NotNull
    String language;

    @Getter
    @Nullable
    List<String> tokens;

    @Getter
    @Nullable
    String text;
}
