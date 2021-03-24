package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.repository.MistakeRepository;
import org.vstu.compprehension.models.entities.MistakeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MistakeService {
    private MistakeRepository mistakeRepository;

    @Autowired
    public MistakeService(MistakeRepository mistakeRepository) {
        this.mistakeRepository = mistakeRepository;
    }
    
    public void saveMistake(MistakeEntity mistake) {
        
        mistakeRepository.save(mistake);
    }
}
