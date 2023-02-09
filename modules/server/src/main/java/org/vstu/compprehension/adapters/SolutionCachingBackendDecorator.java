package org.vstu.compprehension.adapters;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.Collection;
import java.util.List;

/**
 * Additional layer over backend intended to cache results of `solve()`, not
   `judge()`. The cache is limited in size (30 by default) and time after latest access (10 minutes by default).
 * `Statement` piece of input facts should contain a fact of form [meta:question dc:identifier `string`] where `string` is a unique identifier of question being solved (or a set of questions if all of them have the same solution). If no such fact found no caching would be done.
 */
public class SolutionCachingBackendDecorator implements Backend {

    private final @NotNull Backend decoratee;
    private Cache<String /*questionName*/, JenaFactList> solutionCache = null;

    public SolutionCachingBackendDecorator(@NotNull Backend decoratee,
                                           String cacheSpec) {
        this.decoratee = decoratee;
        if (cacheSpec != null)
            initSolutionCache(cacheSpec);
        else
            initSolutionCache();
    }

    private void initSolutionCache() {
        // (... In real life this would come from a command-line flag or config file)
        String spec = "maximumSize=30,expireAfterAccess=10m";
        initSolutionCache(spec);
    }
    private void initSolutionCache(@NotNull String cacheSpec) {
        solutionCache = CacheBuilder.from(cacheSpec).build();
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
        String key = findUniqueKey(statement);
        if (key != null) {
            if (solutionCache == null) {
                initSolutionCache();
            }
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

    private Collection<Fact> askCache(Collection<Fact> statement) {
        if (solutionCache == null) {
            initSolutionCache();
        }

        String key = findUniqueKey(statement);
        if (key == null)
            return null;

        return solutionCache.getIfPresent(key);

    }

    private String findUniqueKey(Collection<Fact> statement) {
        // search for fact of form: [meta:question dc:identifier <string VALUE>]
        if (statement instanceof JenaFactList) {
            JenaFactList fl = (JenaFactList) statement;
            Model m = fl.getModel();

            return m.listStatements(
                    m.createResource(m.expandPrefix("dc:identifier")),
                    m.createProperty(m.expandPrefix("meta:question")),
                    (String) null).toList().stream()
                    .findFirst()
                    .map(Statement::getString) // object is a string
                    .orElse(null);
        }

        return statement.stream()
                .filter(t -> t.getVerb().equals("dc:identifier") && t.getSubject().equals("meta:question"))
                .findFirst()
                .map(Fact::getObject)
                .orElse(null);
    }
}
