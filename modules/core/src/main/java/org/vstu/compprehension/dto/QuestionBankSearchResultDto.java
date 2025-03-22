package org.vstu.compprehension.dto;

import java.util.List;

public record QuestionBankSearchResultDto(long count, long topRatedCount, List<QuestionMetadataDto> questions) {
    public record QuestionMetadataDto(int metadataId, String name) {}
}
