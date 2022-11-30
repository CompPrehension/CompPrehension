package org.vstu.compprehension.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.repository.CtrlFlowQuestionMetadataRepository;

@Service
public class CtrlFlowQuestionService {
    private CtrlFlowQuestionMetadataRepository ctrlFlowQuestionMetadataRepository;

    @Autowired
    public CtrlFlowQuestionService(CtrlFlowQuestionMetadataRepository ctrlFlowQuestionMetadataRepository) {
        this.ctrlFlowQuestionMetadataRepository = ctrlFlowQuestionMetadataRepository;
    }
}
