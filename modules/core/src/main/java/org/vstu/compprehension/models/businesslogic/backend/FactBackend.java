package org.vstu.compprehension.models.businesslogic.backend;

import org.vstu.compprehension.models.businesslogic.DomainToBackendAdapter;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fact-based backend, a superclass for all basic reasoning backends that
 * use {@link Fact} triplets (subject-verb-object) to encode data
 * and {@link Law}s to encode reasoning rules
 *
 * This was previously the main Backend interface
 */
public abstract class FactBackend implements Backend<FactBackend.Input, Collection<Fact>> {

    public record Input(
        List<Law> laws,
        Collection<Fact> statement,
        Collection<Fact> correctAnswer,
        Collection<Fact> response,
        ReasoningOptions reasoningOptions
    ){
        Input(List<Law> laws, Collection<Fact> statement, ReasoningOptions reasoningOptions){
            this(laws, statement, new ArrayList<>(), new ArrayList<>(), reasoningOptions);
        }
    }

    @Override
    public Collection<Fact> judge(Input questionData) {
        return judge(
            questionData.laws,
            questionData.statement,
            questionData.correctAnswer,
            questionData.response,
            questionData.reasoningOptions
        );
    }

    @Override
    public Collection<Fact> solve(Input questionData) {
        return solve(
            questionData.laws,
            questionData.statement,
            questionData.reasoningOptions
        );
    }

    public abstract Collection<Fact> solve(List<Law> laws, List<BackendFactEntity> statement, ReasoningOptions reasoningOptions);

    public abstract Collection<Fact> solve(List<Law> laws, Collection<Fact> statement, ReasoningOptions reasoningOptions);


    /**
     * FIXME? Удалить? не используется
     * @see #judge(List, Collection, Collection, Collection, ReasoningOptions)
     */
    public abstract Collection<Fact> judge(
        List<Law> laws,
        List<BackendFactEntity> statement,
        List<BackendFactEntity> correctAnswer,
        List<BackendFactEntity> response,
        ReasoningOptions reasoningOptions
    );

    public abstract Collection<Fact> judge(
        List<Law> laws,
        Collection<Fact> statement,
        Collection<Fact> correctAnswer,
        Collection<Fact> response,
        ReasoningOptions reasoningOptions
    );

    /* helpers: fact conversion */

    public Fact convertFactEntity(BackendFactEntity factEntity) {
        return convertFact(new Fact(factEntity));
    }
    public Fact convertFact(Fact fact) {
        return fact;
    }
    public Collection<Fact> convertFactEntities(Collection<BackendFactEntity> factEntities) {
        return factEntities.stream().map(this::convertFactEntity).collect(Collectors.toList());
    }
    public Collection<Fact> convertFacts(Collection<Fact> facts) {
        return facts.stream().map(this::convertFact).collect(Collectors.toList());
    }


    public static class Interface<Back extends FactBackend>
        extends DomainToBackendAdapter<Input, Collection<Fact>, Back>
    {
        private final Domain domain;
        private final String backendId;

        public Interface(Class<Back> backendClass, Domain domain, String backendId) {
            super(backendClass);
            this.domain = domain;
            this.backendId = backendId;
        }

        @Override
        public Input prepareBackendInfoForJudge(Question question, List<ResponseEntity> responses, List<Tag> tags) {
            return new Input(
                new ArrayList<>(domain.getQuestionNegativeLaws(question.getQuestionDomainType(), tags)),
                question.getStatementFactsWithSchema(),
                Fact.entitiesToFacts(question.getSolutionFacts()),
                question.responseToFacts(responses),
                new ReasoningOptions(
                    false,
                    domain.getViolationVerbs(question.getQuestionDomainType(), question.getStatementFacts()),
                    question.getQuestionUniqueTemplateName()
                )
            );
        }

        @Override
        public Domain.InterpretSentenceResult interpretJudgeOutput(
            Question judgedQuestion,
            Collection<Fact> backendOutput
        ) {
            Domain.InterpretSentenceResult result = domain.interpretSentence(backendOutput);

            Language lang;
            try {
                lang = judgedQuestion.getQuestionData().getExerciseAttempt().getUser().getPreferred_language(); // The language currently selected in UI
            } catch (NullPointerException e) {
                lang = Language.RUSSIAN/*ENGLISH*/;  // fallback if it cannot be figured out
            }
            result.explanation = domain.makeExplanation(result.violations, FeedbackType.EXPLANATION, lang);
            return result;
        }

        @Override
        public Input prepareBackendInfoForSolve(Question question, List<Tag> tags) {
            return new Input(
                domain.getQuestionLaws(question.getQuestionDomainType(), tags),
                question.getStatementFactsWithSchema(),
                new ReasoningOptions(
                    false,
                    domain.getSolutionVerbs(question.getQuestionDomainType(), question.getStatementFacts()),
                    question.getQuestionUniqueTemplateName()
                ));
        }

        @Override
        public void updateQuestionAfterSolve(Question question, Collection<Fact> solution) {
            List<BackendFactEntity> storedSolution = question.getQuestionData().getSolutionFacts();
            if (storedSolution != null && !storedSolution.isEmpty()) {
                // add anything set as solution before
                solution.addAll(Fact.entitiesToFacts(storedSolution));
            }
            // save facts to question
            question.getQuestionData().setSolutionFacts(Fact.factsToEntities(solution));
        }

        @Override
        public String getBackendId() {
            return backendId;
        }
    }
}
