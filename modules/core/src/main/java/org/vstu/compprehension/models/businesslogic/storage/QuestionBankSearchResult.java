package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.data.QuestionMetadataData;
import org.vstu.compprehension.models.data.SearchQuality;

import java.util.List;

/** Что банк нашёл по запросу и насколько хорошо. */
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
