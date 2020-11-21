package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.businesslogic.Law;
import com.example.demo.models.entities.BackendFact;

import java.util.List;

public abstract class Backend {
    public abstract List<BackendFact> judge(List<Law> laws, List<BackendFact> statement, List<BackendFact> correctAnswer, List<BackendFact> response, List<String> violationVerbs);
    public abstract List<BackendFact> solve(List<Law> laws, List<BackendFact> statement, List<String> solutionVerbs);
}
