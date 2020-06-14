package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.DomainLawViolation;

import java.util.List;

public abstract class Backend {
    
   public abstract List<DomainLawViolation> judge(List<DomainLaw> laws, List<BackendFact> problem, List<BackendFact> response);
}
