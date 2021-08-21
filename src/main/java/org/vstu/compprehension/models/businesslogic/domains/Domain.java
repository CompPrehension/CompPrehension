package org.vstu.compprehension.models.businesslogic.domains;

import org.apache.commons.lang3.tuple.Pair;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.HyperText;

import java.util.*;

public abstract class Domain {
    protected List<PositiveLaw> positiveLaws;
    protected List<NegativeLaw> negativeLaws;
    protected List<Concept> concepts;

    /**
     * Db entry
     */
    protected DomainEntity domainEntity;

    /**
     * domain name (used to get domain by name)
     */
    protected String name = "";
    /**
     * version of domain (in db)
     */
    protected String version = "";

    /**
     * Function for update all internal domain db info
     */
    public abstract void update();

    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }

    public List<PositiveLaw> getPositiveLaws() {
        return positiveLaws;
    }
    public List<NegativeLaw> getNegativeLaws() {
        return negativeLaws;
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public PositiveLaw getPositiveLaw(String name) {
        for (PositiveLaw law : positiveLaws) {
            if (name.equals(law.getName())) {
                return law;
            }
        }
        return null;
    }

    public NegativeLaw getNegativeLaw(String name) {
        for (NegativeLaw law : negativeLaws) {
            if (name.equals(law.getName())) {
                return law;
            }
        }
        return null;
    }

    public Concept getConcept(String name) {
        for (Concept concept : concepts) {
            if (name.equals(concept.getName())) {
                return concept;
            }
        }
        return null;
    }

    /**
     * More interactions a student does, greater possibility to mistake accidentally. A certain rate of mistakes (say 1 of 12) can be considered unintentional so no penalty is assessed.
     * @return the rate threshold
     */
    public double getAcceptableRateOfIgnoredMistakes() {
        return 0.0834;  // = 1/12
    }

    public Domain() {
    }

    /**
     * Get text description of all steps to right solution
     * @param question tested question
     * @return list of step descriptions
     */
    public abstract List<HyperText> getFullSolutionTrace(Question question);

    /**
     * TODO: do we need this function?
     * @return
     */
    public abstract ExerciseForm getExerciseForm();

    /**
     * TODO: do we need this function?
     * @param ef
     * @return
     */
    public abstract ExerciseEntity processExerciseForm(ExerciseForm ef);

    /**
     * Generate domain question with restrictions
     * @param questionRequest params of needed question
     * @param tags question tags (like programming language)
     * @param userLanguage question decription language
     * @return generated question
     */
    public abstract Question makeQuestion(QuestionRequest questionRequest, List<Tag> tags, Language userLanguage);

    /**
     * Generate explanation of violations
     * @param violations list of student violations
     * @param feedbackType TODO: use feedbackType or delete it
     * @param lang user preferred language
     * @return explanation for each violation in random order
     */
    public abstract List<HyperText> makeExplanation(List<ViolationEntity> violations, FeedbackType feedbackType, Language lang);

    /**
     * Get all needed (positive and negative) laws in this questionType
     * @param questionDomainType type of question
     * @param tags question tags
     * @return list of laws
     */
    public List<Law> getQuestionLaws(String questionDomainType, List<Tag> tags) {
        List<PositiveLaw> positiveLaws = getQuestionPositiveLaws(questionDomainType, tags);
        List<NegativeLaw> negativeLaws = getQuestionNegativeLaws(questionDomainType, tags);
        List<Law> laws = new ArrayList<>();
        laws.addAll(positiveLaws);
        laws.addAll(negativeLaws);
        return laws;
    }

    /**
     * Get positive needed laws in this questionType
     * @param questionDomainType type of question
     * @param tags question tags
     * @return list of positive laws
     */
    public abstract List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags);
    /**
     * Get negative needed laws in this questionType
     * @param questionDomainType type of question
     * @param tags question tags
     * @return list of negative laws
     */
    public abstract List<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags);

    /**
     * Get all needed solution BackendFactEntity verbs for db saving
     * @param questionDomainType type of question
     * @param statementFacts question statement facts
     * @return list of BackendFactEntity verbs needed for solve question
     */
    public abstract List<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts);
    /**
     * Get all needed violation BackendFactEntity verbs for db saving
     * @param questionDomainType type of question
     * @param statementFacts question statement facts
     * @return list of BackendFactEntity verbs needed for interpret question
     */
    public abstract List<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts);

    /**
     * Сформировать из ответов (которые были ранее добавлены к вопросу)
     * студента факты в универсальной форме
     * @return - факты в универсальной форме
     */
    public abstract List<BackendFactEntity> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects);

    /**
     * Statistics for current step of question evaluation
     */
    public static class ProcessSolutionResult {
        /**
         * Number of correct variants at current step
         */
        public int CountCorrectOptions;
        /**
         * Shortest number of steps (iterations) left
         */
        public int IterationsLeft;
    }

    /**
     * Info about one iteration
     */
    public static class InterpretSentenceResult extends ProcessSolutionResult {
        /**
         * All violations
         */
        public List<ViolationEntity> violations;
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

    /**
     * Evaluate one iteration, collect info and find violations
     * @param violations violation facts
     */
    public abstract InterpretSentenceResult interpretSentence(List<BackendFactEntity> violations);

    /**
     * Make supplementary question based on violation in last iteration
     * @param violation info about mistake
     * @param sourceQuestion source question
     * @return supplementary question
     */
    public abstract Question makeSupplementaryQuestion(QuestionEntity sourceQuestion, ViolationEntity violation, Language lang);

    public abstract InterpretSentenceResult judgeSupplementaryQuestion(Question question, AnswerObjectEntity answer);


    /**
     * Get statistics for initial step of question evaluation
     * @param solution solution backend facts
     */
    public abstract ProcessSolutionResult processSolution(List<BackendFactEntity> solution);

    /**
     * Any available correct answer at current iteration
     */
    public class CorrectAnswer {
        /**
         * Question
         */
        public QuestionEntity question;
        /**
         * Correct answer objects
         */
        public List<Pair<AnswerObjectEntity, AnswerObjectEntity>> answers;
        /**
         * Text explanation why it chosen
         */
        public HyperText explanation;
        /**
         * Positive law name for this answer
         */
        public String lawName;
    }

    /**
     * Get any correct answer at current iteration
     * @param q question
     * @return any correct answer
     */
    public abstract CorrectAnswer getAnyNextCorrectAnswer(Question q);

    /**
     * Return all question templates
     */
    protected abstract List<Question> getQuestionTemplates();

    /**
     * Find new question template in db similar to given
     * @param tags question tags
     * @param q similar question
     * @return new question template
     */
    public Question findQuestion(List<Tag> tags, Question q) {
        return findQuestion(tags, new HashSet<>(q.getConcepts()), new HashSet<>(), new HashSet<>(q.getNegativeLaws()), new HashSet<>(), new HashSet<>(Set.of(q.getQuestionName())));
    }

    /**
     * Find new question template in db
     * @param tags question tags
     * @param targetConcepts concepts that should be in question
     * @param deniedConcepts concepts that should not be in question
     * @param targetNegativeLaws negative laws that should be in question
     * @param deniedNegativeLaws negative laws that should not be in question
     * @param forbiddenQuestions texts of question that not suit TODO: use ExerciseAttemptEntity
     * @return new question template
     */
    public Question findQuestion(List<Tag> tags, HashSet<String> targetConcepts, HashSet<String> deniedConcepts, HashSet<String> targetNegativeLaws, HashSet<String> deniedNegativeLaws, HashSet<String> forbiddenQuestions) {
        List<Question> questions = new ArrayList<>();
        int maxSuitCount = -999999;
        for (Question q : getQuestionTemplates()) {
            int targetConceptCount = 0;
            int anotherConcepts = 0;
            boolean suit = true;
            if (forbiddenQuestions.contains(q.getQuestionName())) {
                continue;
            }
            for (Tag tag : tags) {
                if (!q.getTags().contains(tag.getName())) {
                    suit = false;
                    break;
                }
            }
            for (String concept : q.getConcepts()) {
                if (deniedConcepts.contains(concept)) {
                    suit = false;
                    break;
                } else if (targetConcepts.contains(concept)) {
                    targetConceptCount++;
                } else {
                    anotherConcepts++;
                }
            }
            for (String negativeLaw : q.getNegativeLaws()) {
                if (deniedNegativeLaws.contains(negativeLaw)) {
                    suit = false;
                    break;
                } else if (targetNegativeLaws.contains(negativeLaw) || targetConcepts.contains(negativeLaw)) {
                    targetConceptCount++;
                } else {
                    anotherConcepts++;
                }
            }
            int questionScore = targetConceptCount - anotherConcepts;
            if (suit && questionScore >= maxSuitCount) {
                if (questionScore > maxSuitCount) {
                    questions.clear();
                    maxSuitCount = questionScore;
                }
                questions.add(q);
            }
        }
        if (questions.isEmpty()) {
            return null;
        } else {
            /// <debug>
            for (Question question : questions) {
                System.out.println("Отобранный вопрос (из " + questions.size() + "): " + question.getQuestionName());
            }
            /// </debug>

            Question question = questions.get(new Random().nextInt(questions.size()));
            /// <debug>
            System.out.println("В итоге, взят опрос: " + question.getQuestionName());
            /// </debug>
            return question;
        }
    }

    public abstract List<Concept> getLawConcepts(Law law);
}
