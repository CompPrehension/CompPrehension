package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.data.questionbank.QuestionBankSearchStatsData;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchStatsDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class QuestionBankSearchStatsDtoMapper implements Mapper<QuestionBankSearchStatsData, QuestionBankSearchStatsDto> {

    @Override
    public @NotNull QuestionBankSearchStatsDto map(@NotNull QuestionBankSearchStatsData source) {
        return new QuestionBankSearchStatsDto(
                source.count(),
                source.topRatedCount(),
                source.found().stream().map(this::map).toList());
    }

    private @NotNull QuestionBankSearchStatsDto.QuestionMetadataDto map(@NotNull QuestionMetadataData source) {
        return new QuestionBankSearchStatsDto.QuestionMetadataDto(source.getId(), source.getName());
    }
}
