package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EducationResourceEntity saveOrGetExisting(EducationResourceEntity entity) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            return repository.findByUrlAndType(entity.getUrl(), entity.getType())
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to saveOrGetExisting EducationResource and could not find existing one", e));
        }
    }
}
