package org.vstu.compprehension.data.questionbank;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.QuestionMetadataData;

import java.util.List;

public record QuestionBankSearchStatsData(long count, long topRatedCount,
                                          @NotNull List<QuestionMetadataData> found) {
}
