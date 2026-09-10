package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.entities.DomainEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;

@Component
class DomainMapper implements Mapper<DomainEntity, DomainData> {

    @Override
    public @NotNull DomainData map(@NotNull DomainEntity source) {
        String name = Strict.required(source.getName(), "name", "domain");
        String owner = "domain " + name;
        return new DomainData(
                name,
                Strict.required(source.getShortName(), "shortName", owner),
                Strict.required(source.getVersion(), "version", owner),
                Strict.required(source.getOptions(), "options", owner));
    }
}
