package org.vstu.compprehension.models.businesslogic.domains;

import org.vstu.compprehension.models.data.SupplementaryStepData;
import org.vstu.compprehension.models.data.ViolationData;
import org.vstu.compprehension.models.data.ExerciseStageData;
import org.vstu.compprehension.models.data.BackendFactData;
import its.reasoner.nodes.DecisionTreeTrace;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.data.QuestionMetadataData;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.data.AnswerObjectData;
import org.vstu.compprehension.models.data.ExerciseOptionsData;
import org.vstu.compprehension.models.data.DomainData;
import org.vstu.compprehension.models.data.QuestionData;
import org.vstu.compprehension.models.data.QuestionMetadataData;
import org.vstu.compprehension.models.data.ResponseData;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.InteractionType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.HyperText;

import java.util.*;

public interface Domain {
    @NotNull String getDomainId();
    @NotNull String getName();
    @NotNull String getShortName();
    @NotNull String getDisplayName(Language language);
    @Nullable String getDescription(Language language);

    /**
     * Get domain-defined backend id, which determines the backend used to SOLVE/JUDGE this domain's questions
     */
    @NotNull String getBackendId();

    /**
     * Этап упражнения, на котором задан вопрос; пусто, если вопрос вне попытки.
     * <p>
     * {@code ExerciseStageData} вопреки имени не JPA-сущность, а значение из
     * json-колонки, поэтому возвращается как есть.
     */
    Optional<ExerciseStageData> getExerciseStageOf(@NotNull Question question);

    /** Описание предметной области: имя, короткое имя, версия, опции. */
    @NotNull DomainData getDomainData();

    /**
     * A temporary method to reuse DB-stored questions between Domains
     * Is the same as {@link #getShortName()} by default
     * FIXME - replace back to getShortName()
     */
    @NotNull String getShortnameForQuestionSearch();

    @NotNull Map<String, Tag> getTags();
    @NotNull List<Tag> getAllTags();
    @Nullable Tag getTag(@NotNull String name);

    /**
     * Сформировать из ответов студента (которые были ранее добавлены к вопросу)
     * факты в универсальной форме
     * @return - факты в универсальной форме
     */
    Collection<Fact> responseToFacts(Question question, List<ResponseData> responses);

    /** Get statement facts with common domain definitions for reasoning (schema) added */
    Collection<Fact> getQuestionStatementFactsWithSchema(Question q);

    /**
     * Get all needed violation Fact verbs for db saving
     *
     * @param questionDomainType type of question
     * @param statementFacts     question statement facts
     */
    Set<String> getViolationVerbs(String questionDomainType, List<BackendFactData> statementFacts);

    /**
     * Get all needed solution Fact verbs for db saving
     *
     * @param questionDomainType type of question
     * @param statementFacts     question statement facts
     */
    Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactData> statementFacts);

    /**
     * Get all needed (positive and negative) laws in this questionType
     * @param questionDomainType type of question
     * @param tags question tags
     * @return list of laws
     */
    List<Law> getQuestionLaws(String questionDomainType, List<Tag> tags);

    /**
     * Get negative needed laws in this questionType
     * @param questionDomainType type of question
     * @param tags question tags
     * @return list of negative laws
     */
    Collection<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags);

    /**
     * Get positive needed laws in this questionType
     * @param questionDomainType type of question
     * @param tags question tags
     * @return list of positive laws
     */
    Collection<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags);

    /**
     * Evaluate one iteration, collect info and find violations
     * @param violations violation facts
     */
    InterpretSentenceResult interpretSentence(Collection<Fact> violations);

    /**
     * Generate explanation of violations
     * @param violations list of student violations
     * @param feedbackType TODO: use feedbackType or delete it
     * @param lang user preferred language
     * @return explanation for each violation in random order
     */
    Explanation makeExplanation(List<ViolationData> violations, FeedbackType feedbackType, Language lang);

    /**
     * Check that violation has supplementary questions
     * @param violationLawName name of the violated law
     * @param interactionType   type of the interaction the violation was detected in, if known
     * @return violation has supplementary questions
     */
    boolean needSupplementaryQuestion(String violationLawName, @Nullable InteractionType interactionType);

    Collection<Concept> getConcepts();
    @Nullable Concept getConcept(String name);
    String getConceptDisplayName(String conceptName, Language language);

    Collection<PositiveLaw> getPositiveLaws();
    Collection<NegativeLaw> getNegativeLaws();
    @Nullable Law getLaw(String name);
    String getLawDisplayName(String lawName, Language language);

    @Nullable Skill getSkill(String name);
    String getSkillDisplayName(String skillName, Language language);

    @Nullable String getDefaultQuestionType();
    @Nullable String getDefaultQuestionType(boolean supplementary);
    List<Tag> getDefaultQuestionTags(String questionDomainType);
    Collection<Concept> getConceptWithChildren(String name_);
    Collection<Concept> getChildrenOfConcept(String name_);

    List<PositiveLaw> getPositiveLawWithImplied(String name);
    List<NegativeLaw> getNegativeLawWithImplied(String name);

    QuestionRequest ensureQuestionRequestValid(QuestionRequest questionRequest);

    /**
     * Generate domain question with restrictions
     * @param questionRequest params of needed question
     * @param userLanguage question wording language
     * @return generated question
     */
    @NotNull Question makeQuestion(@NotNull QuestionRequest questionRequest,
                                   @Nullable ExerciseOptionsData exerciseOptions,
                                   @NotNull Language userLanguage);

    /**
     * Generate domain question from question data
     * @param metadata question metadata
     * @param userLang question wording language
     * @return generated question
     */
    @NotNull Question makeQuestion(@NotNull QuestionMetadataData metadata,
                                   @NotNull List<Tag> tags,
                                   @NotNull Language userLang);

    /**
     * Make supplementary question based on violation in last iteration
     * @param violation info about mistake
     * @param sourceQuestion source question
     * @return supplementary question
     */
    SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionData sourceQuestion, ViolationData violation, Language lang);

    SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(Question question, SupplementaryStepData supplementaryStep, List<ResponseData> responses, Language language);

    /**
     * Get any correct answer at current iteration
     * @param q question
     * @return any correct answer
     */
    CorrectAnswer getAnyNextCorrectAnswer(Question q, Language language);

    /**
     * Get text description of all steps to right solution
     * @param question tested question
     * @return list of step descriptions
     */
    List<HyperText> getFullSolutionTrace(Question question, Language language);

    /** Get concepts with given flags (e.g. visible) organized into two-level hierarchy
     * @param requiredFlags e.g. Concept.FLAG_VISIBLE_TO_TEACHER
     * @return map representing groups of concepts (base concept -> concepts in the group)
     */
    Map<Concept, List<Concept>> getConceptsSimplifiedHierarchy(int requiredFlags);

    /** Get laws with given flags (e.g. visible) organized into two-level hierarchy
     * @param requiredFlags e.g. Law.FLAG_VISIBLE_TO_TEACHER
     * @return map representing groups of laws (base law -> laws in the group)
     */
    Map<Law, List<Law>> getLawsSimplifiedHierarchy(int requiredFlags);

    /** Get skills organized into one-level hierarchy
     * @return map representing groups of skills (base skill -> skills in the group)
     */
    Map<Skill, List<Skill>> getSkillSimplifiedHierarchy(int bitflags);

    Question solveQuestion(Question question, List<Tag> tags);

    /**
     * @param question current question being solved
     * @param responses new responses from student (to add to solution if correct)
     * @param tags Exercise tags
     * @return interpretation of backend's judgement
     */
    InterpretSentenceResult judgeQuestion(Question question, List<ResponseData> responses, List<Tag> tags, Language language);

    /**
     * Any available correct answer at current iteration
     */
    class CorrectAnswer {
        /**
         * Question
         */
        public QuestionData question;
        /**
         * Correct answer objects
         */
        public List<Response> answers;
        /**
         * Text explanation why it has chosen
         */
        public Explanation explanation;
        /**
         * Positive law name for this answer
         */
        public String lawName;
        /**
         * Skill names for this answer
         */
        public List<String> skillName;

        @AllArgsConstructor
        @Data
        public static class Response {
            private AnswerObjectData left;
            private AnswerObjectData right;
        }
    }

    /**
     * Statistics for current step of question evaluation
     */
    class ProcessSolutionResult {
        /**
         * Number of correct variants at current step
         */
        public int CountCorrectOptions;
        /**
         * Shortest number of steps (iterations) left
         */
        public int IterationsLeft;

        /**
         * For debug purposes
         */
        public Map<String, String> debugInfo = new HashMap<>();
    }
    /**
     * Info about one iteration
     */
    class InterpretSentenceResult extends ProcessSolutionResult {
        /**
         * All violations
         */
        public List<ViolationData> violations;

        public List<String> domainSkills = new ArrayList<>();

        public List<String> domainNegativeLaws = new ArrayList<>();

        public Explanation explanation;
        /**
         * List of all negative laws that not occurred
         * (all answers where this answer would be the cause of the violation)
         */
        public List<String> correctlyAppliedLaws;
        /**
         * Is answer on question is correct.
         * Supplementary can generate new violations even on correct variant.
         */
        public boolean isAnswerCorrect;

        /**
         * The trace of reasoning along the decision tree
         */
        @Nullable
        public DecisionTreeTrace decisionTreeTrace = null;
    }
}
