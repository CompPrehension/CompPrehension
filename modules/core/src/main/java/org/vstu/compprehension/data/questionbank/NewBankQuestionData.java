package org.vstu.compprehension.data.questionbank;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.data.question.QuestionMetadataData;

import java.util.List;

/**
 * Вопрос, который генератор кладёт в банк: одно тело и метаданные, его описывающие.
 * <p>
 * Метаданных несколько, потому что один шаблон порождает вопрос на нескольких языках
 * программирования, и тело у них общее. Связка «тело + его метаданные» выражена типом
 * именно поэтому: по отдельности эти записи не имеют смысла, а порядок вставки —
 * сначала тело, потом ссылающиеся на него метаданные — свойство схемы, а не генератора.
 */
public record NewBankQuestionData(@NotNull SerializableQuestion body,
                                  @NotNull List<QuestionMetadataData> metadata) {
}
