package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

    @Transactional
    public EducationResourceEntity createOrGetExisting(EducationResourceEntity entity) {
        repository.createIfAbsent(entity);
        return repository.findByUrlAndType(entity.getUrl(), entity.getType())
                .orElseThrow(() -> new IllegalStateException("createIfAbsent: entity not found after insert"));
    }
}

