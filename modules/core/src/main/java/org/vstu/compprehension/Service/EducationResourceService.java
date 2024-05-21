package org.vstu.compprehension.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;
import org.vstu.compprehension.models.repository.EducationResourceRepository;

import java.util.Optional;

@Service
public class EducationResourceService {
    private final EducationResourceRepository educationResourceRepository;

    @Autowired
    public EducationResourceService(EducationResourceRepository educationResourceRepository) {
        this.educationResourceRepository = educationResourceRepository;
    }

    public EducationResourceEntity getOrCreateEducationResource(String url, String name) {

        Optional<EducationResourceEntity> found = educationResourceRepository.findByUrl(url);
        if (found.isPresent()) {
            return found.get();
        }

        EducationResourceEntity educationResource = new EducationResourceEntity();
        educationResource.setUrl(url);
        educationResource.setName(name);
        return educationResourceRepository.save(educationResource);
    }
}
