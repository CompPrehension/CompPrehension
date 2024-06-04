package org.vstu.compprehension.adapters;

import com.google.common.cache.Cache;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.JenaBackend;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.backends.Backend;
import org.vstu.compprehension.models.businesslogic.backends.ReasoningOptions;
import org.vstu.compprehension.models.businesslogic.backends.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backends.facts.JenaFactList;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.Collection;
import java.util.List;

/**
 * Additional layer over backend intended to cache results of `solve()`, not
   `judge()`. The cache is limited in size (30 by default) and time after latest access (10 minutes by default).
 * `Statement` piece of input facts should contain a fact of form [meta:question dc:identifier `string`] where `string` is a unique identifier of question being solved (or a set of questions if all of them have the same solution). If no such fact found no caching would be done.
 */
@SuppressWarnings("UnstableApiUsage")
public class SolutionCachingJenaBackendDecorator implements Backend {

    private final @NotNull JenaBackend decoratee;
    private final Cache<String /*solutionKey*/, JenaFactList> solutionCache;

    public SolutionCachingJenaBackendDecorator(@NotNull JenaBackend decoratee,
                                               @NotNull Cache<String, JenaFactList> jenaCache) {
        this.decoratee     = decoratee;
        this.solutionCache = jenaCache;
    }

    @NotNull
    @Override
    public String getBackendId() {
        return decoratee.getBackendId();
    }

    @Override
    public Collection<Fact> solve(List<Law> laws, List<BackendFactEntity> statement, ReasoningOptions reasoningOptions) {
        return solve(laws, JenaFactList.fromBackendFacts(statement), reasoningOptions);
    }

    @Override
    public Collection<Fact> solve(List<Law> laws, Collection<Fact> statement, ReasoningOptions reasoningOptions) {
        // ask cache
        String key = reasoningOptions.getUniqueSolutionKey();
        if (key != null) {
            JenaFactList cachedSolution = solutionCache.getIfPresent(key);
            if (cachedSolution != null)
                return cachedSolution;
        }

        // solve
        Collection<Fact> solved = decoratee.solve(laws, statement, reasoningOptions);

        // store in cache
        JenaFactList fl = JenaFactList.fromFacts(solved);
        if (key != null) {
            solutionCache.put(key, fl);
        }
        return fl;
    }

    @Override
    public Collection<Fact> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, ReasoningOptions reasoningOptions) {
        return decoratee.judge(laws, statement, correctAnswer, response, reasoningOptions);
    }

    @Override
    public Collection<Fact> judge(List<Law> laws, Collection<Fact> statement, Collection<Fact> correctAnswer, Collection<Fact> response, ReasoningOptions reasoningOptions) {
        return decoratee.judge(laws, statement, correctAnswer, response, reasoningOptions);
    }

}
