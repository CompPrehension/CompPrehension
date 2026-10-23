package org.vstu.compprehension.frontend.dto;

import java.util.List;

public record QuestionBankSearchStatsDto(long count, long topRatedCount, List<QuestionMetadataDto> questions) {
    public record QuestionMetadataDto(int metadataId, String name) {}
}
