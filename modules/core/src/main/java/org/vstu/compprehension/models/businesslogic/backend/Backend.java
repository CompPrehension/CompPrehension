package org.vstu.compprehension.models.businesslogic.backend;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface Backend {
    @NotNull String getBackendId();

    Collection<Fact> solve(List<Law> laws, List<BackendFactEntity> statement, ReasoningOptions reasoningOptions);

    Collection<Fact> solve(List<Law> laws, Collection<Fact> statement, ReasoningOptions reasoningOptions);


    Collection<Fact> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, ReasoningOptions reasoningOptions);
    Collection<Fact> judge(List<Law> laws, Collection<Fact> statement, Collection<Fact> correctAnswer, Collection<Fact> response, ReasoningOptions reasoningOptions);

    /* helpers: fact conversion */

    default Fact convertFactEntity(BackendFactEntity factEntity) {
        return convertFact(new Fact(factEntity));
    }
    default Fact convertFact(Fact fact) {
        return fact;
    }
    default Collection<Fact> convertFactEntities(Collection<BackendFactEntity> factEntities) {
        return factEntities.stream().map(this::convertFactEntity).collect(Collectors.toList());
    }
    default Collection<Fact> convertFacts(Collection<Fact> facts) {
        return facts.stream().map(this::convertFact).collect(Collectors.toList());
    }
}
