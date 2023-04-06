package org.vstu.compprehension.models.businesslogic.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage;
import org.vstu.compprehension.models.businesslogic.storage.LocalRdfStorage;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataBaseRepository;
import org.vstu.compprehension.models.repository.QuestionRequestLogRepository;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@RequestScope
public abstract class Domain {
    public static final String NAME_PREFIX_IS_HUMAN = "[human]";
    protected  Map<String, PositiveLaw> positiveLaws;
    protected  Map<String, NegativeLaw> negativeLaws;

    /** name to Concept mapping */
    protected Map<String, Concept> concepts;

    /** name to Tag mapping */
    protected Map<String, Tag> tags;

    protected AbstractRdfStorage rdfStorage;
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
    @Getter
    protected final RandomProvider randomProvider;
    protected final QuestionRequestLogRepository questionRequestLogRepository;



    public Domain(RandomProvider randomProvider, QuestionRequestLogRepository questionRequestLogRepository) {
        this.randomProvider = randomProvider;
        this.questionRequestLogRepository = questionRequestLogRepository;
    }

    /**
     * Function for update all internal domain db info
     */
    public abstract void update();

    public String getName() {
        return name;
    }
    public String getShortName() {
        return name;  // same as name by default
    }

    public String getVersion() {
        return version;
    }

    public DomainEntity getEntity() {
        return domainEntity;
    }

    public Collection<PositiveLaw> getPositiveLaws() {
        return positiveLaws.values();
    }
    public Collection<NegativeLaw> getNegativeLaws() {
        return negativeLaws.values();
    }

    public Collection<Concept> getConcepts() {
        return concepts.values();
    }

    public PositiveLaw getPositiveLaw(String name) {
        return positiveLaws.getOrDefault(name, null);
    }

    public NegativeLaw getNegativeLaw(String name) {
        return negativeLaws.getOrDefault(name, null);
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

    public Tag getTag(String name) {
        return tags.getOrDefault(name, null);
    }

    /** Get concepts with given flags (e.g. visible) organized into two-level hierarchy
     * @param requiredFlags e.g. Concept.FLAG_VISIBLE_TO_TEACHER
     * @return map representing groups of concepts (base concept -> concepts in the group)
     */
    public Map<Concept, List<Concept>> getConceptsSimplifiedHierarchy(int requiredFlags) {
        Map<Concept, List<Concept>> res = new HashMap<>();
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
        // set direct child Laws to Laws
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

    public Model getSchemaForSolving(/* String questionType (?) */) {
        // the default
        return ModelFactory.createDefaultModel();
    }

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

    public AbstractRdfStorage getRdfStorage() {
        if (rdfStorage == null) {
            rdfStorage = new LocalRdfStorage(this);
        }
        return rdfStorage;
    }

    public QuestionMetadataBaseRepository getQuestionMetadataRepository() {
        return null;
    }

    public void saveQuestionRequest(QuestionRequest qr) {

        // fill empty lists
        if (qr.getDeniedQuestionMetaIds().isEmpty())
            qr.getDeniedQuestionMetaIds().add(0);
        if (qr.getDeniedQuestionTemplateIds().isEmpty())
            qr.getDeniedQuestionTemplateIds().add(0);

        val qrl = qr.getLogEntity();

        Map<String, Object> res = getQuestionMetadataRepository().countQuestions(qr);
        int questionsFound = ((BigInteger)res.getOrDefault("number", -2)).intValue();
        qrl.setFoundCount(questionsFound);
        qrl.setCreatedDate(new Date());
        questionRequestLogRepository.save(qrl);
    }

    abstract public Question parseQuestionTemplate(InputStream stream);

    /**
     * More interactions a student does, greater possibility to mistake accidentally. A certain rate of mistakes (say 1 of 12) can be considered unintentional so no penalty is assessed.
     * @return the rate threshold
     */
    public double getAcceptableRateOfIgnoredMistakes() {
        return 0.0834;  // = 1/12
    }

    public abstract @NotNull String getDomainId();

    /**
     * Get text description of all steps to right solution
     * @param question tested question
     * @return list of step descriptions
     */
    public abstract List<HyperText> getFullSolutionTrace(Question question);

    /**
     * Get text description of all steps to complete correct solution
     * @param question not solved question
     * @return list of step descriptions
     */
    public List<HyperText> getCompleteSolvedTrace(Question question) {
        return List.of();
    }

    public List<CorrectAnswer> getAllAnswersOfSolvedQuestion(Question question) {
        return List.of();
    }

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
     * @param userLanguage question wording language
     * @return generated question
     */
    public abstract Question makeQuestion(QuestionRequest questionRequest, List<Tag> tags, Language userLanguage);

    /** Convert lists of concepts and laws to bitmasks */
    public QuestionRequest fillBitmasksInQuestionRequest(QuestionRequest qr) {
        qr.setConceptsTargetedBitmask(conceptsToBitmask(qr.getTargetConcepts()));
//        qr.setConceptsAllowedBitmask(conceptsToBitmask(qr.getAllowedConcepts()));  // unused ?
        qr.setConceptsDeniedBitmask(conceptsToBitmask(qr.getDeniedConcepts()));
        qr.setConceptsTargetedInPlanBitmask(conceptsToBitmask(qr.getTargetConceptsInPlan()));

        qr.setLawsTargetedBitmask(lawsToBitmask(qr.getTargetLaws()));
//        qr.setAllowedLawsBitmask(awsToBitmask(qr.getAllowedLaws()));  // unused ?
        qr.setLawsDeniedBitmask(lawsToBitmask(qr.getDeniedLaws()));
        qr.setLawsTargetedInPlanBitmask(lawsToBitmask(qr.getTargetLawsInPlan()));

        return qr;
    }

    protected static long conceptsToBitmask(List<Concept> concepts) {
        long conceptBitmask = 0; //
        for (Concept t : concepts) {
            long newBit = t.getBitmask();
            if (newBit == 0) {
                // make use of children
                newBit = t.getSubTreeBitmask();
            }
            conceptBitmask |= newBit;
        }
        return conceptBitmask;
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
        Collection<PositiveLaw> positiveLaws = getQuestionPositiveLaws(questionDomainType, tags);
        Collection<NegativeLaw> negativeLaws = getQuestionNegativeLaws(questionDomainType, tags);
        List<Law> laws = new ArrayList<>();
        laws.addAll(positiveLaws);
        laws.addAll(negativeLaws);
        return laws;
    }

    /** Get all needed (positive and negative) laws for this questionType using default tags */
    public abstract List<Law> getQuestionLaws(String questionDomainType);

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
    public abstract Question makeSupplementaryQuestion(QuestionEntity sourceQuestion, ViolationEntity violation, Language lang);

    public abstract InterpretSentenceResult judgeSupplementaryQuestion(Question question, AnswerObjectEntity answer);


    /**
     * Get statistics for initial step of question evaluation
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
     * Get set of mistakes that can be made by a student when solving remaining part of the task (or whole task if stepsPassed is null or empty)
     * @param q question
     * @param completedSteps ignore mistakes possible in these steps
     * @return set of negative law names (i.e. mistakes)
     */
    public abstract Set<String> possibleViolations(Question q, List<ResponseEntity> completedSteps);

    /** Shortcut to `possibleViolations(question, completedSteps=null)`
     * @param q
     * @return
     */
    public Set<String> possibleViolations(Question q) {
        return possibleViolations(q, null);
    }

    /**
     * Get set of sets of mistakes that can be made by a student when solving remaining part of the task (or whole task if stepsPassed is null or empty)
     * @param q question
     * @param completedSteps ignore mistakes possible in these steps
     * @return set of sets of negative law names (i.e. mistakes)
     */
    public abstract Set<Set<String>> possibleViolationsByStep(Question q, List<ResponseEntity> completedSteps);

    /** Shortcut to `possibleViolationsByStep(question, completedSteps=null)`
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
                log.info("Отобранный вопрос (из " + questions.size() + "): " + question.getQuestionName());
            }

            Question question = questions.get(randomProvider.getRandom().nextInt(questions.size()));
            log.info("В итоге, взят вопрос: " + question.getQuestionName());

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


    //    API домена для обработки template’ов, которое будет вызываться службой хранилища
    /** Compute inferences from question template by calling reasoner with appropriate rules.
     * Returned RDF model should not include triples that exist in template or schema .
     * @param templateName may be useful
     * @param questionTemplate the main data to use
     * @param domainSchema (pre-solved) ready-made RDF model to feed into reasoner with main data
     * */
    Model solveQuestionTemplateRDF(String templateName, Model questionTemplate, Model domainSchema) {
        return null;
    }

    /** Compute inferences for (generated) question by calling reasoner with appropriate rules.
     * Returned RDF model should not include triples that exist in template or schema .
     * @param questionName (pre-solved) may be useful
     * @param questionData includes (solved) template and basic data about question
      * @param domainSchema ready-made RDF model to feed into reasoner with main data
     * */
    Model solveQuestionRDF(String questionName, Model questionData, Model domainSchema) {
        return null;
    }


    /** Генерирует вопросы из шаблона, подбирая вопросы с разными наборам ошибок, минимальные по длине решения.
     * Generate questions from a template, selecting questions with different sets of errors, and minimal solutions in length.
     *
     * @param templateName may be useful to make final question names
     * @param solvedTemplate all known data about question template
     * @param domainSchema (pre-solved) may be useful
     * @param questionsLimit maximum questions to create (avoiding infinite loops)
     * @return map: [new question name] -> [contents of QUESTION graph]
     * */
    public Map<String, Model> generateDistinctQuestions(String templateName, Model solvedTemplate, Model domainSchema, int questionsLimit) {
        return null;
    }

}
