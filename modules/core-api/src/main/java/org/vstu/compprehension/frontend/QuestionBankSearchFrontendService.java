package org.vstu.compprehension.frontend;

import org.vstu.compprehension.frontend.dto.QuestionBankSearchRequestDto;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchStatsDto;

public interface QuestionBankSearchFrontendService {
    QuestionBankSearchStatsDto search(QuestionBankSearchRequestDto searchRequest);
}
