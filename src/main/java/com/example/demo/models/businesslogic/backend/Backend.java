package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.DomainLawViolation;
import com.example.demo.models.entities.Law;
import com.example.demo.models.entities.Mistake;
import com.example.demo.utils.HyperText;

import java.util.List;

public abstract class Backend {

    public abstract List<Mistake> judge(List<Law> laws, HyperText problem, List<BackendFact> statement, List<BackendFact> response);
}
