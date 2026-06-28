package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceTrustStatus;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.models.repository.EducationResourceRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EducationResourceService {

    private final EducationResourceRepository repository;

    @Transactional(readOnly = true)
    public Optional<EducationResourceEntity> findByUrlAndType(String url, EducationResourceType type) {
        return repository.findByUrlAndType(url, type);
    }

    @Transactional
    public EducationResourceEntity createOrGetExisting(String url, EducationResourceType type) {
        repository.createIfAbsent(url, type.name());
        return repository.findByUrlAndType(url, type)
                .orElseThrow(() -> new IllegalStateException("createIfAbsent: entity not found after insert"));
    }

    /**
     * Возвращает образовательный ресурс по (url, type), создавая его при отсутствии, и проверяет,
     * что он доверенный. Бросает {@link SecurityException}, если ресурс ещё не переведён в
     * {@link EducationResourceTrustStatus#TRUSTED} — до этого момента LTI-привязка и работа с курсами
     * запрещены (approval-gate).
     */
    @Transactional
    public EducationResourceEntity getOrCreateTrusted(String url, EducationResourceType type) {
        EducationResourceEntity eduRes = findByUrlAndType(url, type)
                .orElseGet(() -> createOrGetExisting(url, type));
        if (eduRes.getTrustStatus() != EducationResourceTrustStatus.TRUSTED) {
            throw new SecurityException(String.format("EducationResource %s is not trusted", eduRes.getUrl()));
        }
        return eduRes;
    }
}

