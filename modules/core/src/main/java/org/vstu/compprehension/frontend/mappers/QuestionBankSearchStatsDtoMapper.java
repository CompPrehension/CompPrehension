package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchStatsDto;
import org.vstu.compprehension.mappers.Mapping;

import java.util.List;

public interface QuestionBankSearchStatsDtoMapper extends Mapping {

    @NotNull QuestionBankSearchStatsDto map(long count, long topRatedCount,
                                            @NotNull List<QuestionMetadataData> found);
}
