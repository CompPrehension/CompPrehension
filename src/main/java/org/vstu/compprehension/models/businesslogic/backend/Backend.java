package org.vstu.compprehension.models.businesslogic.backend;

import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.List;

public abstract class Backend {
    public abstract List<BackendFactEntity> solve(List<Law> laws, List<BackendFactEntity> statement, List<String> solutionVerbs);
//    public abstract SerializedRDF solve(List<Law> laws, SerializedRDF problem, List<String> solutionVerbs);


    public abstract List<BackendFactEntity> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, List<String> violationVerbs);
//    public abstract List<BackendFactEntity> judge(List<Law> laws, List<BackendFactEntity> statement, SerializedRDF solution, List<BackendFactEntity> response, List<String> sentenceVerbs);
}
