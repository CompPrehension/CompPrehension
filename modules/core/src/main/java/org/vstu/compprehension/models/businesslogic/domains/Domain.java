package org.vstu.compprehension.models.businesslogic.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.FactBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@RequestScope
public abstract class Domain {
    public static final String NAME_PREFIX_IS_HUMAN = "[human]";
    protected  Map<String, PositiveLaw> positiveLaws;
    protected  Map<String, NegativeLaw> negativeLaws;

    /** name to Concept mapping */
    protected Map<String, Concept> concepts;

    /**
     * domain name (used to get domain by name)
     */

    /**
     * version of domain (in db)
     */
    protected String version = "";
    @Getter
    protected final RandomProvider randomProvider;
    @Getter
    private final DomainEntity domainEntity;

    public Domain(DomainEntity domainEntity, RandomProvider randomProvider) {
        this.domainEntity = domainEntity;
        this.randomProvider = randomProvider;
    }

    public @NotNull String getDomainId() {
        return domainEntity.getName();
    }
    public String getName() {
        return domainEntity.getName();
    }
    public String getShortName() {
        return domainEntity.getShortName();  // same as name by default
    }

    /**
     * A temporary method to reuse DB-stored questions between Domains
     * Is the same as {@link #getShortName()} by default
     * FIXME - replace back to getShortName()
     */
    public String getShortnameForQuestionSearch(){
        return getShortName();
    }
    public String getVersion() {
        return version;
    }
    public abstract @NotNull String getDisplayName(Language language);
    public abstract @Nullable String getDescription(Language language);
    public DomainOptionsEntity getOptions() { return domainEntity.getOptions(); }

    public DomainEntity getEntity() {
        return domainEntity;
    }

    public @Nullable Tag getTag(@NotNull String name) {
        return getTags().get(name);
    }
    public abstract @NotNull Map<String, Tag> getTags();

    public Collection<PositiveLaw> getPositiveLaws() {
        return positiveLaws.values();
    }
    public Collection<NegativeLaw> getNegativeLaws() {
        return negativeLaws.values();
    }

    public Collection<Concept> getConcepts() {
        return concepts.values();
    }
    public String getConceptDisplayName(String conceptName, Language language) {
        return getMessage(conceptName, "concept.", language);
    }

    public String getLawDisplayName(String lawName, Language language) {
        return getMessage(lawName, "law.", language);
    }

    public @Nullable PositiveLaw getPositiveLaw(String name) {
        return positiveLaws.getOrDefault(name, null);
    }

    public @Nullable NegativeLaw getNegativeLaw(String name) {
        return negativeLaws.getOrDefault(name, null);
    }

    public @Nullable Law getLaw(String name) {
        var negative = getNegativeLaw(name);
        if (negative != null)
            return negative;
        return getPositiveLaw(name);
    }

    public Collection<? extends Law> getLawWithChildren(String name_) {
        return getLawsWithChildren(List.of(name_));
    }

    public Collection<? extends Law> getLawsWithChildren(Collection<String> names) {
        Set<String> res = new HashSet<>();
        Set<String> pool = new HashSet<>(names);
        while (!pool.isEmpty()) {
            // copy concepts from pool to res
            res.addAll(pool);

            for (String name : new HashSet<>(pool)) {
                pool.remove(name);
                Law currLaw = getLaw(name);
                if (currLaw != null && currLaw.getChildLaws() != null) {
                    // try to add all children of current concept
                    pool.addAll(currLaw.getChildLaws().stream().map(Law::getName).collect(Collectors.toSet()));
                    pool.removeAll(res);  // guard: don't allow infinite recursion.
                }
            }
        }
        return res.stream().map(this::getLaw).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public List<PositiveLaw> getPositiveLawWithImplied(String name) {
        PositiveLaw law = getPositiveLaw(name);
        if (law == null)
            return List.of();

        List<PositiveLaw> res = new ArrayList<>(List.of(law));
        List<String> impliedNames = law.getImpliesLaws();
        if (impliedNames == null)
            return res;

        for (String lawName : impliedNames) {
            // danger: infinite recursion is possible
            res.addAll(getPositiveLawWithImplied(lawName));
        }
        return res;
    }
    public List<NegativeLaw> getNegativeLawWithImplied(String name) {
        if (name == null)
            return List.of();
        NegativeLaw law = getNegativeLaw(name);
        if (law == null)
            return List.of();

        List<NegativeLaw> res = new ArrayList<>(List.of(law));
        List<String> impliedNames = law.getImpliesLaws();
        if (impliedNames == null)
            return res;

        for (String lawName : impliedNames) {
            // danger: infinite recursion is possible
            res.addAll(getNegativeLawWithImplied(lawName));
        }
        return res;
    }

    public Concept getConcept(String name) {
        return concepts.getOrDefault(name, null);
    }

    /** Get concepts with given flags (e.g. visible) organized into two-level hierarchy
     * @param requiredFlags e.g. Concept.FLAG_VISIBLE_TO_TEACHER
     * @return map representing groups of concepts (base concept -> concepts in the group)
     */
    public Map<Concept, List<Concept>> getConceptsSimplifiedHierarchy(int requiredFlags) {
        Map<Concept, List<Concept>> res = new TreeMap<>();
        Set<Concept> wanted = this.concepts.values().stream().filter(t -> t.hasFlag(requiredFlags)).collect(Collectors.toSet());
        Set<Concept> added = new HashSet<>();
        for (Concept ct : new ArrayList<>(wanted)) {
            // ensure we are dealing with bottom-level concept
            Collection<Concept> children = this.getConceptWithChildren(ct.getName());
            children.remove(ct);
            boolean hasChildren =
                    children.stream().anyMatch(wanted::contains);
            if (hasChildren) {
                continue;  // skip non-bottom concepts
            }

            Concept nearestWantedBase = null;
            List<Concept> bases = new ArrayList<>(ct.getBaseConcepts());
            while (!bases.isEmpty()) {
                for (Concept base : new ArrayList<>(bases)) {
                    if (wanted.contains(base)) {
                        nearestWantedBase = base;
                        bases.clear();
                        break;
                    }
                    bases.remove(base);
                    bases.addAll(base.getBaseConcepts());
                }
            }
            Concept key;
            List<Concept> value;

            if (nearestWantedBase != null) {
                // concept is within a group
                key = nearestWantedBase;
                value = new ArrayList<>(List.of(ct));
                added.add(ct);

            } else {
                // concept does not belong to any group (has no bases we want)
                key = ct;
                value = new ArrayList<>();
            }
            // put into a group or as top-level
            if (res.containsKey(key)) {
                List<Concept> arr = res.get(key);
                for (Concept oneValue : value)
                    if (!arr.contains(oneValue)) {
                        arr.add(oneValue);
                        added.add(oneValue);
                    }
            } else {
                res.put(key, value);
                added.add(key);
            }
        }
        // add all top-level bases we skipped
        wanted.removeAll(added);
        for (Concept t : wanted) {
            res.put(t, new ArrayList<>());
        }

        // sort list items
        for (var list : res.values()) {
            list.sort(TreeNodeWithBitmask::compareTo);
        }

        return res;
    }

    /** Get laws with given flags (e.g. visible) organized into two-level hierarchy
     * @param requiredFlags e.g. Law.FLAG_VISIBLE_TO_TEACHER
     * @return map representing groups of laws (base law -> laws in the group)
     */
    public Map<Law, List<Law>> getLawsSimplifiedHierarchy(int requiredFlags) {
        Map<Law, List<Law>> res = new TreeMap<>();
        Set<Law> wanted = Stream.concat(this.getPositiveLaws().stream(), this.getNegativeLaws().stream())
                .filter(t -> t.hasFlag(requiredFlags)).collect(Collectors.toSet());
        Set<Law> added = new HashSet<>();
        for (Law ct : new ArrayList<>(wanted)) {
            // ensure we are dealing with bottom-level law
            Collection<Law> children = (Collection<Law>) this.getLawWithChildren(ct.getName());
            children.remove(ct);
            boolean hasChildren =
                    children.stream().anyMatch(wanted::contains);
            if (hasChildren) {
                continue;  // skip non-bottom laws
            }

            Law nearestWantedBase = null;
            List<Law> bases = new ArrayList<>(ct.getLawsImplied());
            while (!bases.isEmpty()) {
                for (Law base : new ArrayList<>(bases)) {
                    if (wanted.contains(base)) {
                        nearestWantedBase = base;
                        bases.clear();
                        break;
                    }
                    bases.remove(base);
                    bases.addAll(base.getLawsImplied());
                }
            }
            Law key;
            List<Law> value;

            if (nearestWantedBase != null) {
                // law is within a group
                key = nearestWantedBase;
                value = new ArrayList<>(List.of(ct));
                added.add(ct);

            } else {
                // law does not belong to any group (has no bases we want)
                key = ct;
                value = new ArrayList<>();
            }
            // put into a group or as top-level
            if (res.containsKey(key)) {
                List<Law> arr = res.get(key);
                for (Law oneValue : value)
                    if (!arr.contains(oneValue)) {
                        arr.add(oneValue);
                        added.add(oneValue);
                    }
            } else {
                res.put(key, value);
                added.add(key);
            }
        }
        // add all top-level bases we skipped
        wanted.removeAll(added);
        for (Law t : wanted) {
            res.put(t, new ArrayList<>());
        }

        // sort list items
        for (var list : res.values()) {
            list.sort(TreeNodeWithBitmask::compareTo);
        }

        return res;
    }

    public Collection<Concept> getChildrenOfConcept(String name_) {
        return getConceptsWithChildren(List.of(name_)).stream()
                .filter(t -> !name_.equals(t.getName()))
                .collect(Collectors.toList());
    }

    public Collection<Concept> getChildrenOfConcepts(Collection<String> names) {
        return getConceptsWithChildren(names).stream()
                .filter(t -> !names.contains(t.getName()))
                .collect(Collectors.toSet());
    }

    public Collection<Concept> getConceptWithChildren(String name_) {
        return getConceptsWithChildren(List.of(name_));
    }

    public Collection<Concept> getConceptsWithChildren(Collection<String> names) {
        Set<String> res = new HashSet<>();
        Set<String> pool = new HashSet<>(names);
        while (!pool.isEmpty()) {
            // copy concepts from pool to res
            res.addAll(pool);  // .stream().map(this::getConcept).collect(Collectors.toSet()));
//            res.addAll(pool.stream().flatMap(n -> tm.get(n).stream()).collect(Collectors.toSet()));
            for (String name : new HashSet<>(pool)) {
                pool.remove(name);
                Concept currConcept =  concepts.getOrDefault(name, null);
                if (currConcept != null && currConcept.getChildConcepts() != null) {
                    // try to add all children of current concept
                    pool.addAll(currConcept.getChildConcepts().stream().map(Concept::getName).collect(Collectors.toSet()));
                    pool.removeAll(res);  // guard: don't allow infinite recursion.
                }
            }
        }
        return res.stream().map(this::getConcept).filter(Objects::nonNull).collect(Collectors.toSet());
//        return new ArrayList<>(res);
    }


    protected Concept addConcept(Concept t) {
        concepts.put(t.getName(), t);
        return t;
    }
    protected Concept addConcept(String name, List<Concept> bases, int flags) {
        return addConcept(new Concept(name, bases, flags));
    }

    protected void addConcepts(Collection<Concept> ts) {
        for (Concept t : ts)
            addConcept(t);
    }

    /** Set direct children to concepts. This is needed since parents (bases) of concepts are stored only */
    protected void fillConceptTree() {
        for (Concept concept : concepts.values()) {
            if (concept.getBaseConcepts() == null)
                continue;
            for (Concept base : concept.getBaseConcepts()) {
                if (base.getChildConcepts() == null) {
                    base.setChildConcepts(new HashSet<>());
                }
                base.getChildConcepts().add(concept);
            }
        }
    }

    /** Set direct children to both positive and negative Laws. This is needed since names of laws are stored only */
    protected void fillLawsTree() {
        // set direct implied (base) Laws to each Law
        for (Law t : positiveLaws.values()) {
            if (t.getImpliesLaws() == null) {
                t.setLawsImplied(List.of());
            } else {
                t.setLawsImplied(t.getImpliesLaws().stream().map(this::getPositiveLaw).filter(Objects::nonNull).collect(Collectors.toSet()));
            }
        }
        for (Law t : negativeLaws.values()) {
            if (t.getImpliesLaws() == null) {
                t.setLawsImplied(List.of());
            } else {
                t.setLawsImplied(t.getImpliesLaws().stream().map(this::getNegativeLaw).filter(Objects::nonNull).collect(Collectors.toSet()));
            }
        }

        // set direct child Laws to Laws
        var allLaws = Stream.concat(getPositiveLaws().stream(), getNegativeLaws().stream()).collect(Collectors.toSet());
        for (Law law : allLaws) {
            if (law.getLawsImplied() == null)
                continue;
            for (Law base : law.getLawsImplied()) {
                if (base.getChildLaws() == null) {
                    base.setChildLaws(new HashSet<>());
                }
                base.getChildLaws().add(law);
            }
        }

    }


    /**
     * Get localized domain-specific string
     * @param messageKey language string key
     * @param preferredLanguage target language (fallback is english)
     * @return localized message
     */
    public abstract String getMessage(String messageKey, Language preferredLanguage);

    /**
     * Get localized domain-specific string
     * @param messageKey language string key
     * @param prefix prefix for language key (kind of namespace)
     * @param preferredLanguage target language (fallback is english)
     * @return localized message
     */
    public String getMessage(String messageKey, String prefix, Language preferredLanguage) {
        return getMessage(prefix + messageKey, preferredLanguage);
    }

    /** Get statement facts with common domain definitions for reasoning (schema) added */
    public abstract Collection<Fact> getQuestionStatementFactsWithSchema(Question q);

    public String getDefaultQuestionType() {
        return getDefaultQuestionType(false);
    }

    public String getDefaultQuestionType(boolean supplementary) {
        return null;  // the default
    }

    public List<Tag> getDefaultQuestionTags(String questionDomainType) {
        // the default
        return new ArrayList<>();
    }

    /**
     * Get text description of all steps to right solution
     * @param question tested question
     * @return list of step descriptions
     */
    public abstract List<HyperText> getFullSolutionTrace(Question question);

    /**
     * Generate domain question with restrictions
     * @param questionRequest params of needed question
     * @param tags question tags (like programming language)
     * @param userLanguage question wording language
     * @return generated question
     */
    public abstract Question makeQuestion(ExerciseAttemptEntity exerciseAttempt, QuestionRequest questionRequest, List<Tag> tags, Language userLanguage);

    public QuestionRequest ensureQuestionRequestValid(QuestionRequest questionRequest) {
        return questionRequest;
    }

    protected static long lawsToBitmask(List<Law> laws) {
        long lawBitmask = 0;
        // Note: violations are not positive laws.
        for (Law t : laws) {
            long newBit = t.getBitmask();
            if (newBit == 0) {
                // make use of children
                newBit = t.getSubTreeBitmask();
            }
            lawBitmask |= newBit;
        }
        return lawBitmask;
    }

    /**
     * Get domain-defined backend id, which determines the backend used to SOLVE this domain's questions
     */
    public abstract String getSolvingBackendId();

    /**
     * Get domain-defined backend id, which determines the backend used to JUDGE this domain's questions. By default, the same as solving domain.
     */
    public String getJudgingBackendId(/* TODO: pass question type ??*/){
        return this.getSolvingBackendId();
    }

    /**
     * Generate explanation of violations
     * @param violations list of student violations
     * @param feedbackType TODO: use feedbackType or delete it
     * @param lang user preferred language
     * @return explanation for each violation in random order
     */
    public abstract List<HyperText> makeExplanation(List<ViolationEntity> violations, FeedbackType feedbackType, Language lang);

    public List<HyperText> makeExplanations(List<Fact> reasonerOutputFacts, Language lang){
        return null;
    }

    /**
     * Get all needed (positive and negative) laws in this questionType
     * @param questionDomainType type of question
     * @param tags question tags
     * @return list of laws
     */
    public List<Law> getQuestionLaws(String questionDomainType, List<Tag> tags) {
        Collection<PositiveLaw> positiveLaws = getQuestionPositiveLaws(questionDomainType, tags);
        Collection<NegativeLaw> negativeLaws = getQuestionNegativeLaws(questionDomainType, tags);
        List<Law> laws = new ArrayList<>();
        laws.addAll(positiveLaws);
        laws.addAll(negativeLaws);
        return laws;
    }

    /** Check if a law needed for a question with tags specified.
     * Returns true in two cases:
     * 1) the law has no tags attached, or
     * 2) the two sets of tag names do intersect (i.e. contain at least one common tag).
     * @param law Law to check tags for
     * @param tags Tags to check against.
     * @return true if any common tag exists.
     */
    public static boolean isLawNeededByQuestionTags(Law law, Collection<Tag> tags) {
        boolean needLaw = true;
        for (Tag tag : law.getTags()) {  // law having no tags is still needed.
            boolean inQuestionTags = false;
            for (Tag questionTag : tags) {
                if (questionTag.getName().equals(tag.getName())) {
                    inQuestionTags = true;
                    break;
                }
            }
            if (!inQuestionTags) {
                needLaw = false;
                break;
            }
        }
        return needLaw;
    }

    /** Get all needed (positive and negative) laws for this questionType using default tags */
    //LOOK public abstract List<Law> getQuestionLaws(String questionDomainType);

    /**
     * Get positive needed laws in this questionType
     * @param questionDomainType type of question
     * @param tags question tags
     * @return list of positive laws
     */
    public abstract Collection<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags);
    /**
     * Get negative needed laws in this questionType
     * @param questionDomainType type of question
     * @param tags question tags
     * @return list of negative laws
     */
    public abstract Collection<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags);

    /**
     * Get all needed solution Fact verbs for db saving
     *
     * @param questionDomainType type of question
     * @param statementFacts     question statement facts
     * @return list of Fact verbs needed for solve question
     */
    public abstract Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts);
    /**
     * Get all needed violation Fact verbs for db saving
     *
     * @param questionDomainType type of question
     * @param statementFacts     question statement facts
     * @return list of Fact verbs needed for interpret question
     */
    public abstract Set<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts);

    /**
     * Сформировать из ответов студента (которые были ранее добавлены к вопросу)
     * факты в универсальной форме
     * @return - факты в универсальной форме
     */
    public abstract Collection<Fact> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects);

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

    /**
     * Evaluate one iteration, collect info and find violations
     * @param violations violation facts
     */
    public abstract InterpretSentenceResult interpretSentence(Collection<Fact> violations);

    /**
     * Check that violation has supplementary questions
     * @param violation info about mistake
     * @return violation has supplementary questions
     */
    public abstract boolean needSupplementaryQuestion(ViolationEntity violation);

    /**
     * Make supplementary question based on violation in last iteration
     * @param violation info about mistake
     * @param sourceQuestion source question
     * @return supplementary question
     */
    public abstract SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionEntity sourceQuestion, ViolationEntity violation, Language lang);

    public abstract SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(Question question, SupplementaryStepEntity supplementaryStep, List<ResponseEntity> responses);


    /**
     * Get statistics for initial step of question evaluation
     * FIXME? Удалить? Используется только внутри {@link #interpretSentence}, нет смысла объявлять как часть интерфейса
     *
     * @param solution solution backend facts
     */
    public abstract ProcessSolutionResult processSolution(Collection<Fact> solution);

    /**
     * Any available correct answer at current iteration
     */
    public static class CorrectAnswer {
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

        @AllArgsConstructor @Data
        public static class Response {
            private AnswerObjectEntity left;
            private AnswerObjectEntity right;
        }
    }

    /**
     * Get any correct answer at current iteration
     * @param q question
     * @return any correct answer
     */
    public abstract CorrectAnswer getAnyNextCorrectAnswer(Question q);

    /**
     * FIXME? Удалить?
     *
     * Get set of mistakes that can be made by a student when solving remaining part of the task (or whole task if stepsPassed is null or empty)
     * @param q question
     * @param completedSteps ignore mistakes possible in these steps
     * @return set of negative law names (i.e. mistakes)
     */
    public abstract Set<String> possibleViolations(Question q, List<ResponseEntity> completedSteps);

    /** Shortcut to `possibleViolations(question, completedSteps=null)`
     * FIXME? Удалить?
     *
     * @param q
     * @return
     */
    public Set<String> possibleViolations(Question q) {
        return possibleViolations(q, null);
    }

    /**
     * FIXME? Удалить?
     *
     * Get set of sets of mistakes that can be made by a student when solving remaining part of the task (or whole task if stepsPassed is null or empty)
     * @param q question
     * @param completedSteps ignore mistakes possible in these steps
     * @return set of sets of negative law names (i.e. mistakes)
     */
    public abstract Set<Set<String>> possibleViolationsByStep(Question q, List<ResponseEntity> completedSteps);

    /** Shortcut to `possibleViolationsByStep(question, completedSteps=null)`
     * FIXME? Удалить?
     *
     * @param q
     * @return
     */
    public Set<Set<String>> possibleViolationsByStep(Question q) {
        return possibleViolationsByStep(q, null);
    }


    /**
     * Find minimum of steps to perform automatically for student, in order to obtain question state when target violations are possible.
     *  (nextStepWithPossibleViolations - дано - один из сетов ошибок и частичный ответ пользователя, найти - ближайшее к этому ответу состояние с нужным сетом ошибок)
     * @return list of responses to activate (empty list if no actions are required); null if the desired state is not available till the end of the question.
    */
    public List<ResponseEntity> findNextStepWithPossibleViolations(Set<String> targetViolations, Question q, List<ResponseEntity> completedSteps) {
        return null;
    }


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
     * Find a question template in in-memory suite of Domain's `questions`
     * @param tags question tags
     * @param targetConcepts concepts that should be in question
     * @param deniedConcepts concepts that should not be in question
     * @param targetNegativeLaws negative laws that should be in question
     * @param deniedNegativeLaws negative laws that should not be in question
     * @param forbiddenQuestions texts of question that not suit TODO: use ExerciseAttemptEntity
     * @return new question template
     */
    public Question findQuestion(List<Tag> tags, Set<String> targetConcepts, Set<String> deniedConcepts, Set<String> targetNegativeLaws, Set<String> deniedNegativeLaws, Set<String> forbiddenQuestions) {
        List<Question> questions = new ArrayList<>();

        int maxSuitCount = 0;
        int minAdditionalCount = 10000;
        for (Question q : getQuestionTemplates()) {
            int targetConceptCount = 0;
            int anotherConcepts = 0;
            boolean suit = true;
            if (forbiddenQuestions.contains(q.getQuestionName()) || forbiddenQuestions.contains(NAME_PREFIX_IS_HUMAN + q.getQuestionName())) {
                continue;
            }
            for (Tag tag : tags) {
                if (!q.getTags().contains(tag.getName())) {
                    suit = false;
                    break;
                }
            }
            if (!suit) continue;
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
            if (!suit) continue;
            for (String negativeLaw : q.getNegativeLaws()) {
                if (deniedNegativeLaws.contains(negativeLaw)) {
                    suit = false;
                    break;
                } else if (targetNegativeLaws.contains(negativeLaw)) {
                    targetConceptCount++;
                } else {
                    anotherConcepts++;
                }
            }
            if (suit) {
                if (targetConceptCount > maxSuitCount || targetConceptCount == maxSuitCount && anotherConcepts <= minAdditionalCount) {
                    if (targetConceptCount > maxSuitCount || anotherConcepts < minAdditionalCount) {
                        questions.clear();
                        maxSuitCount = targetConceptCount;
                        minAdditionalCount = anotherConcepts;
                    }
                    questions.add(q);
                }
            }
        }
        if (questions.isEmpty()) {
            return null;
        } else {
            for (Question question : questions) {
                log.info("Отобранный вопрос (из {}): {}", questions.size(), question.getQuestionName());
            }

            Question question = questions.get(randomProvider.getRandom().nextInt(questions.size()));
            log.info("В итоге, взят вопрос: {}", question.getQuestionName());

            ///
            /// add a mark to the question's name: this question is made by human.
            if (question.getQuestionName() != null && ! question.getQuestionName().startsWith(NAME_PREFIX_IS_HUMAN) ) {
                question.getQuestionData().setQuestionName(NAME_PREFIX_IS_HUMAN + question.getQuestionName());
            }
            ///

            return question;
        }
    }

    public abstract List<Concept> getLawConcepts(Law law);

    //-----

    /**
     * Define all the {@link DomainToBackendInterface} this domain supports
     */
    protected Set<DomainToBackendInterface<?, ?, ?>> createBackendInterfaces(){
        return Set.of(
            new FactBackend.Interface<>(JenaBackend.class, this)
        );
    }

    private final Map<Class<?>, DomainToBackendInterface<?, ?, ?>> backendClassToInterfaceMap =
        createBackendInterfaces().stream()
            .collect(Collectors.toMap(
                DomainToBackendInterface::getBackendClass,
                Function.identity()
            ));

    /**
     * Get an interface instance which this Domain uses to interact with a given backend <br>
     * Returns null if no such interface exists
     * - however, this situation should not be possible if the system is working correctly
     */
    public final DomainToBackendInterface<?, ?, ?> getBackendInterface(Backend<?, ?> backend){
        return backendClassToInterfaceMap.get(
            //Unwrapping Spring proxies and decorator backends
            AopProxyUtils.ultimateTargetClass(backend.getActualBackend())
        );
    }
}
