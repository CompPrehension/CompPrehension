package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.repository.data.QuestionBankDataRepository;

/**
 * Слой хранения генератора: ничего не хранит.
 * <p>
 * Генератор работает без базы — он не ищет готовые вопросы, а порождает новые. Банк ему
 * нужен только чтобы собрать домен, поэтому собирается из пустых репозиториев. Сборка
 * живёт здесь, рядом с ними: наружу генератору выходит уже готовый слой доступа к данным.
 */
public final class FakeDataAccess {

    private FakeDataAccess() {
    }

    /** Пустой банк заданий. */
    public static QuestionBankDataRepository questionBank() {
        return new QuestionBankDataRepository(
                new FakeQuestionMetadataRepository(),
                new FakeSerializedQuestionRepository(),
                null,
                null);
    }
}
