package org.vstu.compprehension.models.entities;

import java.util.List;

public class DomainLawViolation {
    
    private long id;
    
    private long violatedLaw;
    
    private List<ExplanationTemplateInfoEntity> explanationTemplatesInfo;
}
