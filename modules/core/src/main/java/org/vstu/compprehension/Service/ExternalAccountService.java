package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.course.ExternalAccountEntity;
import org.vstu.compprehension.models.repository.ExternalAccountRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExternalAccountService {

    private final ExternalAccountRepository repository;

    @Transactional(readOnly = true)
    public Optional<ExternalAccountEntity> findByUserAndEducationResource(Long userId, Long educationResourceId) {
        return repository.findByUserIdAndEducationResourceId(userId, educationResourceId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExternalAccountEntity createOrGetExisting(ExternalAccountEntity account) {
        if (account.getId() != null) {
            throw new IllegalArgumentException("Use save() to update existing ExternalAccountEntity");
        }
        Long userId = account.getUser().getId();
        Long resourceId = account.getEducationResource().getId();
        try {
            return repository.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            return findByUserAndEducationResource(userId, resourceId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to createOrGetExisting ExternalAccount and could not find existing one", e
                    ));
        }
    }
}
