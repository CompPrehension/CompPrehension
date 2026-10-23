package org.vstu.compprehension.frontend;

import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.DomainDto;
import org.vstu.compprehension.frontend.dto.StrategyDto;

import java.util.List;
import java.util.Set;

public interface ReferenceTableFrontendService {
    List<StrategyDto> getStrategies(Language language);

    Set<String> getBackendIds();

    List<DomainDto> getDomains(Language language);
}
