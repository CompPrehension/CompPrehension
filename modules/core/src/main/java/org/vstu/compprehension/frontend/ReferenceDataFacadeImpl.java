package org.vstu.compprehension.frontend;

import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.DomainDto;
import org.vstu.compprehension.frontend.dto.StrategyDto;
import org.vstu.compprehension.frontend.mappers.DomainDtoMapper;
import org.vstu.compprehension.frontend.mappers.StrategyDtoMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReferenceDataFacadeImpl implements ReferenceTableFrontendService {
    private final DomainFactory domainFactory;
    private final AbstractStrategyFactory strategyFactory;
    private final DomainDtoMapper domainDtoMapper;
    private final StrategyDtoMapper strategyDtoMapper;

    public ReferenceDataFacadeImpl(DomainFactory domainFactory,
                                   AbstractStrategyFactory strategyFactory,
                                   DomainDtoMapper domainDtoMapper,
                                   StrategyDtoMapper strategyDtoMapper) {
        this.domainFactory = domainFactory;
        this.strategyFactory = strategyFactory;
        this.domainDtoMapper = domainDtoMapper;
        this.strategyDtoMapper = strategyDtoMapper;
    }

    @Override
    public List<StrategyDto> getStrategies(Language language) {
        return strategyFactory.getStrategyIds().stream()
                .map(strategyFactory::getStrategy)
                .filter(s -> s.getOptions().isVisibleToUser())
                .map(s -> strategyDtoMapper.map(s, language))
                .toList();
    }

    @Override
    public Set<String> getBackendIds() {
        // TODO get backend dor specified domain
        return domainFactory.getDomainIds()
                .stream().map(domainFactory::getDomain)
                .map(Domain::getBackendId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<DomainDto> getDomains(Language language) {
        return domainFactory.getDomainIds().stream()
                .map(domainFactory::getDomain)
                .map(d -> domainDtoMapper.map(d, language))
                .toList();
    }
}
