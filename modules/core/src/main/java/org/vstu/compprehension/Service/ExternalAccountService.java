package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.course.ExternalAccountEntity;
import org.vstu.compprehension.models.entities.course.ExternalAccountId;
import org.vstu.compprehension.models.repository.ExternalAccountRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExternalAccountService {

    private final ExternalAccountRepository repository;

    @Transactional(readOnly = true)
    public Optional<ExternalAccountEntity> findByUserAndEducationResource(Long userId, Long educationResourceId) {
        return repository.findById(new ExternalAccountId(userId, educationResourceId));
    }

    @Transactional
    public ExternalAccountEntity createOrGetExisting(
            Long userId,
            Long educationResourceId,
            String externalId
    ) {
        repository.createIfAbsent(userId, educationResourceId, externalId);
        return repository.findById(new ExternalAccountId(userId, educationResourceId))
                .orElseThrow(() -> new IllegalStateException("createIfAbsent: entity not found after insert"));
    }
}

