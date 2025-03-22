package org.vstu.compprehension.models.businesslogic.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
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
     * Get domain-defined backend id, which determines the backend used to SOLVE this domain's questions
     */
    @NotNull String getSolvingBackendId();

    /**
     * Get domain-defined backend id, which determines the backend used to JUDGE this domain's questions. By default, the same as solving domain.
     */
    @NotNull String getJudgingBackendId();

    @NotNull DomainEntity getDomainEntity();

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
    Collection<Fact> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects);

    /** Get statement facts with common domain definitions for reasoning (schema) added */
    Collection<Fact> getQuestionStatementFactsWithSchema(Question q);

    /**
     * Get all needed violation Fact verbs for db saving
     *
     * @param questionDomainType type of question
     * @param statementFacts     question statement facts
     */
    Set<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts);

    /**
     * Get all needed solution Fact verbs for db saving
     *
     * @param questionDomainType type of question
     * @param statementFacts     question statement facts
     */
    Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts);

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
    List<HyperText> makeExplanation(List<ViolationEntity> violations, FeedbackType feedbackType, Language lang);

    /**
     * Check that violation has supplementary questions
     * @param violation info about mistake
     * @return violation has supplementary questions
     */
    boolean needSupplementaryQuestion(ViolationEntity violation);

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
                                   @Nullable ExerciseAttemptEntity exerciseAttempt,
                                   @NotNull Language userLanguage);

    /**
     * Generate domain question from question data
     * @param metadata question metadata
     * @param exerciseAttemptEntity exercise attempt
     * @param userLang question wording language
     * @return generated question
     */
    @NotNull Question makeQuestion(@NotNull QuestionMetadataEntity metadata,
                                   @Nullable ExerciseAttemptEntity exerciseAttemptEntity,
                                   @NotNull List<Tag> tags,
                                   @NotNull Language userLang);

    /**
     * Make supplementary question based on violation in last iteration
     * @param violation info about mistake
     * @param sourceQuestion source question
     * @return supplementary question
     */
    SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionEntity sourceQuestion, ViolationEntity violation, Language lang);

    SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(Question question, SupplementaryStepEntity supplementaryStep, List<ResponseEntity> responses);

    /**
     * Get any correct answer at current iteration
     * @param q question
     * @return any correct answer
     */
    CorrectAnswer getAnyNextCorrectAnswer(Question q);

    /**
     * Get text description of all steps to right solution
     * @param question tested question
     * @return list of step descriptions
     */
    List<HyperText> getFullSolutionTrace(Question question);

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
    InterpretSentenceResult judgeQuestion(Question question, List<ResponseEntity> responses, List<Tag> tags);


    /**
     * Any available correct answer at current iteration
     */
    class CorrectAnswer {
        /**
         * Question
         */
        public QuestionEntity question;
        /**
         * Correct answer objects
         */
        public List<Response> answers;
        /**
         * Text explanation why it has chosen
         */
        public HyperText explanation;
        /**
         * Positive law name for this answer
         */
        public String lawName;
        /**
         * Skill name for this answer
         */
        public String skillName;

        @AllArgsConstructor
        @Data
        public static class Response {
            private AnswerObjectEntity left;
            private AnswerObjectEntity right;
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
        public List<ViolationEntity> violations;

        public List<String> domainSkills = new ArrayList<>();

        public List<String> domainNegativeLaws = new ArrayList<>();

        public List<HyperText> explanations;
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
    }
}
