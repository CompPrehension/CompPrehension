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
        return repository.findByUser_IdAndEducationResource_Id(userId, educationResourceId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExternalAccountEntity saveOrGetExisting(ExternalAccountEntity entity) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            return repository.findByUser_IdAndEducationResource_Id(
                            entity.getUser().getId(), entity.getEducationResource().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to saveOrGetExisting ExternalAccount and could not find existing one", e));
        }
    }
}
