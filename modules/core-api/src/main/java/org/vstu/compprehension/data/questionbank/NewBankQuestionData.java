package org.vstu.compprehension.data.questionbank;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.data.question.QuestionMetadataData;

import java.util.List;

public record NewBankQuestionData(@NotNull SerializableQuestion body,
                                  @NotNull List<QuestionMetadataData> metadata) {
}
