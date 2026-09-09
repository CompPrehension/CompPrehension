package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchStatsDto;

import java.util.List;

@Component
class QuestionBankSearchStatsDtoMapperImpl implements QuestionBankSearchStatsDtoMapper {

    @Override
    public @NotNull QuestionBankSearchStatsDto map(long count, long topRatedCount,
                                                   @NotNull List<QuestionMetadataData> found) {
        var questions = found.stream()
                .map(m -> new QuestionBankSearchStatsDto.QuestionMetadataDto(m.getId(), m.getName()))
                .toList();
        return new QuestionBankSearchStatsDto(count, topRatedCount, questions);
    }
}
