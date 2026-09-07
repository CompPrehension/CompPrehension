package org.vstu.compprehension.businesslogic.storage;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.data.questionbank.SearchQuality;

import java.util.List;

public class QuestionBankSearchResult {
    @Getter @NotNull
    private final SearchQuality quality;
    @Getter @NotNull
    private final List<QuestionMetadataData> questions;

    public QuestionBankSearchResult(@NotNull SearchQuality quality, @NotNull List<QuestionMetadataData> questions) {
        this.quality = quality;
        this.questions = questions;
    }
}
