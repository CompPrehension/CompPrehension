package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One of input format for expression domain generator
 */
@Getter
@AllArgsConstructor
public class QuestionGeneratedTemplate {
    @NotNull
    String language;

    @Nullable
    List<String> tokens;

    @Nullable
    String text;
}
