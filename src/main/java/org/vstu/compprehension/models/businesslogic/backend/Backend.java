package org.vstu.compprehension.models.businesslogic.backend;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.List;

public interface Backend {
    @NotNull String getBackendId();

    List<BackendFactEntity> solve(List<Law> laws, List<BackendFactEntity> statement, List<String> solutionVerbs);
//    public abstract SerializedRDF solve(List<Law> laws, SerializedRDF problem, List<String> solutionVerbs);


    List<BackendFactEntity> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, List<String> violationVerbs);
//    public abstract List<BackendFactEntity> judge(List<Law> laws, List<BackendFactEntity> statement, SerializedRDF solution, List<BackendFactEntity> response, List<String> sentenceVerbs);
}
