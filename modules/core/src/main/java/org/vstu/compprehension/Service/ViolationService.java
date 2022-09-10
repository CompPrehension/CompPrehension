package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.repository.ViolationRepository;
import org.vstu.compprehension.models.entities.ViolationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ViolationService {
    private ViolationRepository violationRepository;

    @Autowired
    public ViolationService(ViolationRepository violationRepository) {
        this.violationRepository = violationRepository;
    }
    
    public void saveViolation(ViolationEntity mistake) {
        violationRepository.save(mistake);
    }
}
