package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.ExternalAccountData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;
import org.vstu.compprehension.repositories.entity.ExternalAccountRepository.ExternalAccountView;

@Component
class ExternalAccountMapper implements Mapper<ExternalAccountView, ExternalAccountData> {

    @Override
    public @NotNull ExternalAccountData map(@NotNull ExternalAccountView source) {
        long educationResourceId = Strict.required(
                source.getEducationResourceId(), "educationResourceId", "external account");
        String owner = "external account of education resource " + educationResourceId;
        return new ExternalAccountData(
                Strict.required(source.getUserId(), "userId", owner),
                educationResourceId,
                Strict.required(source.getExternalId(), "externalId", owner));
    }
}
