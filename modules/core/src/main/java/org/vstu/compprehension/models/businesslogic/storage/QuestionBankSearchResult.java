package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataSearchRequestEntity;

import java.util.List;

public class QuestionBankSearchResult { 
    @Getter @NotNull
    private final QuestionMetadataSearchRequestEntity.Quality quality;
    @Getter @NotNull
    private final List<QuestionMetadataEntity> questions;

    public QuestionBankSearchResult(@NotNull QuestionMetadataSearchRequestEntity.Quality quality, @NotNull List<QuestionMetadataEntity> questions) {
        this.quality = quality;
        this.questions = questions;
    }
}
