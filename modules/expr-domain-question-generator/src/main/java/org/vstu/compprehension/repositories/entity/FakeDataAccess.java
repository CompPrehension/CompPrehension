package org.vstu.compprehension.repositories.entity;

import org.vstu.compprehension.repositories.data.QuestionBankDataRepository;

public final class FakeDataAccess {

    private FakeDataAccess() {
    }

    /** Пустой банк заданий. */
    public static QuestionBankDataRepository questionBank() {
        return new QuestionBankDataRepository(
                new FakeQuestionMetadataRepository(),
                new FakeSerializedQuestionRepository(),
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
