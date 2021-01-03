package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.businesslogic.Law;
import com.example.demo.models.entities.BackendFactEntity;

import java.util.List;

public abstract class Backend {
    public abstract List<BackendFactEntity> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, List<String> violationVerbs);
    public abstract List<BackendFactEntity> solve(List<Law> laws, List<BackendFactEntity> statement, List<String> solutionVerbs);
}
