package org.vstu.compprehension.models.businesslogic.domains;

import its.model.DomainSolvingModel;
import its.model.definition.VariableDef;
import its.model.definition.rdf.DomainRDFFiller;
import its.model.definition.rdf.DomainRDFWriter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.text.StringSubstitutor;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.utils.ApplicationContextProvider;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;

@Log4j2
public class ControlFlowStatementsDTDomain extends ControlFlowStatementsDomain {
    public static final String LOCALE_KEY_MARK = "!{locale:";
    static final String RESOURCES_LOCATION = "org/vstu/compprehension/models/businesslogic/domains/";
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String EXECUTION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderActsSupplementary";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "control-flow-messages"; // fixme!
    static final String TAG_DECISION_TREE = "decision-tree";
    static final String MESSAGE_PREFIX = "ctrlflow_";
    static final String MESSAGE_DT_SUFFIX = "_DT";  // decision-tree in lang strings.

    private static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "control-flow-statements-domain-model/";

    private static final HashMap<String, Tag> tags = new HashMap<>() {{
        put("C++", new Tag("C++", 2L));  	// (2 ^ 1)
        put("trace", new Tag("trace", 4L));  	// (2 ^ 2)
        put("ordering", new Tag("ordering", 8L));  	// (2 ^ 3)
        put("supplementary", new Tag("supplementary", 16L));  	// (2 ^ 4)
        put("decision-tree", new Tag("decision-tree", 32L));  	// (2 ^ 5)
    }};

//    private static DomainVocabulary VOCAB = null;
//    public static DomainVocabulary getVocabulary() {
//        return VOCAB;
//    }

    public static final String QUESTIONS_CONFIG_PATH = RESOURCES_LOCATION + "control-flow-statements-domain-questions.json"; // fixme!
    static List<Question> QUESTIONS;

//    private static List<String> reasonPropertiesCache = null;


    private static DomainSolvingModel domainSolvingModel = null;
    private void loadDTModel() {
        if (domainSolvingModel == null)
            domainSolvingModel = new DomainSolvingModel(
                this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION), //FIXME
                DomainSolvingModel.BuildMethod.DICT_RDF  // or: .LOQI
            );
    }


    @SneakyThrows
    public ControlFlowStatementsDTDomain(
            DomainEntity domainEntity,
            LocalizationService localizationService,
            RandomProvider randomProvider,
            QuestionBank qMetaStorage
//            QuestionMetadataRepository questionMetadataRepository
    ) {
        super(domainEntity, localizationService, randomProvider, qMetaStorage);
        loadDTModel();
    }

    @NotNull
    @Override
    public String getDisplayName(Language language) {
        return localizationService.getMessage("ctrlflow_text.display_name" + MESSAGE_DT_SUFFIX, language);
    }

    /**
     * Jena is used to SOLVE this domain's questions
     **/
    @Override
    public String getSolvingBackendId() {
        return JenaBackend.BackendId;
    }

    /**
     * Decision Tree Reasoner is used to JUDGE this domain's questions.
     */
    @Override
    public String getJudgingBackendId(/* TODO: pass question type ??*/){
        return DecisionTreeReasonerBackend.BACKEND_ID;
    }

    public String getShortnameForQuestionSearch(){
        return "ctrl_flow";
    }

    @NotNull
    @Override
    public Map<String, Tag> getTags() {
        return tags;
    }

    /**
     * Read laws for reasoning with jena
     * @param inputStream file stream to read from
     */
    @Override
    protected void readLaws(InputStream inputStream) {

        super.readLaws(inputStream);

        // add a new law made from DecisionTree
        loadDTModel();
        var dtLaw = new DTLaw(domainSolvingModel.getDecisionTree());
        negativeLaws.put(dtLaw.getName(), dtLaw);
        // <<
    }

    @Override
    public Question makeQuestion(ExerciseAttemptEntity exerciseAttempt, QuestionRequest questionRequest, List<Tag> tags, Language userLanguage) {

        Question res;

        List<Question> foundQuestions = new ArrayList<>();
        double chance = questionRequest.getChanceToPickAutogeneratedQuestion();
        if (chance == 1.0 ||
                        chance > 0.0 &&
                        randomProvider.getRandom().nextDouble() < chance)
        {
            final int randomPoolSize = 1;  // 16;
            try {
                // new version - invoke rdfStorage search
                questionRequest = ensureQuestionRequestValid(questionRequest);
                foundQuestions = qMetaStorage.searchQuestions(this, exerciseAttempt, questionRequest, randomPoolSize);

                // search again if nothing found with "TO_COMPLEX"
                SearchDirections lawsSearchDir = questionRequest.getLawsSearchDirection();
                if (foundQuestions.isEmpty() && lawsSearchDir == SearchDirections.TO_COMPLEX) {
                    questionRequest.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
                    foundQuestions = qMetaStorage.searchQuestions(this, exerciseAttempt, questionRequest, randomPoolSize);
                }
                log.info("Autogenerated questions found: {}", foundQuestions.size());
            } catch (Exception e) {
                // file storage was not configured properly...
                log.error("Error searching questions - {}", e.getMessage(), e);
                foundQuestions = new ArrayList<>();
            }
        }


        int qN = foundQuestions.size();
        if (qN > 0) {
            if (qN == 1)
                res = foundQuestions.get(0);
            else {
                res = foundQuestions.get(randomProvider.getRandom().nextInt(foundQuestions.size()));
            }
        } else {
            // old version - search in domain's in-memory questions (created manually)
            // Prepare concept name sets ...
            HashSet<String> conceptNames = new HashSet<>();
            for (Concept concept : questionRequest.getTargetConcepts()) {
                conceptNames.add(concept.getName());
            }
            HashSet<String> deniedConceptNames = new HashSet<>();
            for (Concept concept : questionRequest.getDeniedConcepts()) {
                deniedConceptNames.add(concept.getName());
            }
            deniedConceptNames.add("supplementary");

            // Get negative and positive laws names ...
            HashSet<String> lawNames = new HashSet<>();
            if (questionRequest.getTargetLaws() != null) {
                for (Law law : questionRequest.getTargetLaws()) {
                    lawNames.add(law.getName());
                }
            }

            HashSet<String> deniedLawNames = new HashSet<>();
            if (questionRequest.getDeniedLaws() != null) {
                for (Law law : questionRequest.getDeniedLaws()) {
                    deniedLawNames.add(law.getName());
                }
            }

            //        HashSet<String> deniedQuestions = new HashSet<>();
            //        if (questionRequest.getExerciseAttempt() != null && questionRequest.getExerciseAttempt().getQuestions() != null) {
            //            for (QuestionEntity q : questionRequest.getExerciseAttempt().getQuestions()) {
            //                deniedQuestions.add(q.getQuestionName());
            //            }
            //        }

            //        // update QR with denied questions
            //        questionRequest.setDeniedQuestionNames(deniedQuestions.stream().collect(Collectors.toUnmodifiableList()));

            res = findQuestion(tags, conceptNames, deniedConceptNames, lawNames, deniedLawNames, Optional.ofNullable(questionRequest.getDeniedQuestionNames()).map(Set::copyOf).orElse(Set.of()));
        }


        if (res == null) {
            // get anything. TODO: make it input-dependent
            // get (a random) index
            int tryCount = 0;
            do {
                int index = randomProvider.getRandom().nextInt(QUESTIONS.size());
                res = QUESTIONS.get(index);
                tryCount += 1;
            } while (tryCount <= 20  // avoid infinite search
                && questionRequest.getDeniedQuestionNames().contains(res.getQuestionName()));
            ///
            /// add a mark to the question's name: this question is made by human.
            if (res.getQuestionName() != null && ! res.getQuestionName().startsWith(NAME_PREFIX_IS_HUMAN) ) {
                res.getQuestionData().setQuestionName(NAME_PREFIX_IS_HUMAN + res.getQuestionName());
            }
            ///

        }
        Question questionCopy = makeQuestionCopy(res, exerciseAttempt, userLanguage);

        //// patch question text for survey: hide comments
        // questionCopy.getQuestionData().setQuestionText(
        //         questionCopy.getQuestionText().getText().replace(
        //                 "span.comment {",
        //                 "span.comment { display: none;"
        //         )
        // );

        log.info("CtrlFlow domain has prepared the question: {}", questionCopy.getQuestionName());

        return questionCopy;
    }


    /**
     * Generate explanation for individual violation
     *
     * @param violation   violation
     * @param feedbackType TODO: use feedbackType or delete it
     * @param userLang user preferred language
     * @return explanation for given violation
     */
    public HyperText makeExplanation(ViolationEntity violation, FeedbackType feedbackType, Language userLang) {
        String lawName = violation.getLawName();
        String msg = getMessage(lawName, userLang);

        if (msg == null) {
            return new HyperText("[Empty explanation] for law " + lawName);
        }

        // Build replacement map
        Map<String, String> replacementMap = new HashMap<>();
        for (ExplanationTemplateInfoEntity item : violation.getExplanationTemplateInfo()) {
            String value = item.getValue();

            // handle special locale-dependent placeholders
            value = replaceLocaleMarks(userLang, value);
            replacementMap.put(item.getFieldName(), value);
        }

        // Replace in msg
        msg = replaceInString(msg, replacementMap);
        msg = postProcessFormattedMessage(msg);

        return new HyperText(msg);
    }


    @Override
    public List<Tag> getDefaultQuestionTags(String questionDomainType) {
        if (Objects.equals(questionDomainType, EXECUTION_ORDER_QUESTION_TYPE)) {
            return Stream.of("decision-tree", "C++").map(this::getTag).filter(Objects::nonNull).collect(Collectors.toList());
        }
        // empty list by default:
        return super.getDefaultQuestionTags(questionDomainType);
    }

    /*@Override
    public List<Law> getQuestionLaws(String questionDomainType, List<Tag> tags) {
        return Collections.singletonList(new DTLaw(this.domainSolvingModel.getDecisionTree()));
    }*/

    // filter positive laws by question type and tags
    @Override
    public Collection<PositiveLaw> getQuestionPositiveLaws(String domainQuestionType, List<Tag> tags) {
        /// debug OFF
        if (false && tags != null && !tags.isEmpty() && domainQuestionType.equals(EXECUTION_ORDER_QUESTION_TYPE) || domainQuestionType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
            List<PositiveLaw> positiveLaws = new ArrayList<>();
            for (PositiveLaw law : getPositiveLaws()) {
                boolean needLaw = isLawNeededByQuestionTags(law, tags);
                if (needLaw) {
                    positiveLaws.add(law);
                }
            }
            return positiveLaws;
        }
        // return new ArrayList<>(Collections.emptyList());
        return getPositiveLaws();
    }

    public Collection<NegativeLaw> getQuestionNegativeLaws(String domainQuestionType, List<Tag> tags) {
        /// debug OFF
        if (false && tags != null && !tags.isEmpty() && domainQuestionType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            List<NegativeLaw> negativeLaws = new ArrayList<>();
            for (NegativeLaw law : getNegativeLaws()) {
                boolean needLaw = isLawNeededByQuestionTags(law, tags);
                if (needLaw) {
                    negativeLaws.add(law);
                }
            }
            return negativeLaws;
        }
        // return new ArrayList<>(Collections.emptyList());
        return getNegativeLaws();
    }

    //-----------ФАКТЫ---------------

    @Override
    public Collection<Fact> processQuestionFactsForBackendJudge(
            Collection<Fact> questionFacts,
            Collection<ResponseEntity> responses,
            Collection<Fact> responseFacts,
            Collection<Fact> solutionFacts) {
        assert responseFacts != null;
        assert solutionFacts != null;

        // USE Jena: prepare facts for further judging with DT

        JenaBackend backend = new JenaBackend();

//        ch.hit("responseToFacts: schema obtained");

        JenaFactList newFacts = backend.judge(
                new ArrayList<>(this.getQuestionNegativeLaws(EXECUTION_ORDER_QUESTION_TYPE /*<Fixme: only one type of question is supported!*/, null)),
                questionFacts,
                solutionFacts,
                responseFacts,
                new ReasoningOptions(
                        false,
                        this.getViolationVerbs(EXECUTION_ORDER_QUESTION_TYPE, List.of()),
                        null)
        );

        assert newFacts != null;



        // update model so it is ready for reasoning with DT
        OntModel m = ModelFactory.createOntologyModel(OWL_MEM);
        m.add(newFacts.getModel());
        m = backend.filterModelForCtrlFlowDecisionTree(m);
        newFacts.setModel(m);


        // polyfill object descriptions (label -> **.localizedName)
        m = (OntModel) newFacts.getModel();
        for (Statement t : /*Stream.concat*/(
                m.listStatements(null, m.getOntProperty(m.expandPrefix(":stmt_name")), (RDFNode) null).toList().stream()/*,
                m.listStatements(null, m.getOntProperty(m.expandPrefix(":name")), (RDFNode) null).toList().stream()*/
            ).toList()
        ) {
            if (!t.getObject().isLiteral())
                continue;
            String stmt_name = t.getObject().asLiteral().getString();
            if (stmt_name.contains(LOCALE_KEY_MARK)) {
                stmt_name = replaceLocaleMarks(Language.RUSSIAN, stmt_name);  // TODO: use language as preferred by user or webpage.
            } else {
                stmt_name = String.format("«%s»", stmt_name);  // Format name in HTML.
            }
            // stmt_name = String.format("<code>%s</code>", stmt_name);  // Format name in HTML.
            var nameLiteral = m.createLiteral(stmt_name);
            for (String propName : List.of(":RU.localizedName", ":EN.localizedName")) {
                // add a copy of fact with new prop
                m.add(t.getSubject(),
                        m.createOntProperty(m.expandPrefix(propName)),
                        nameLiteral);
            }
        }
        newFacts.setModel(m);


        // use newFacts!

        var model = newFacts.getModel();

        its.model.definition.Domain situationModel = getDTSituationModel(model);


        // fill initial variables.
        var varContainer = situationModel.getDomain().getVariables();

        String iri = m.listStatements(null, RDF.type, m.getOntClass(m.expandPrefix(":algorithm")))
                .nextOptional().map(Statement::getSubject).map(Resource::getURI)
                .orElseThrow();
        String objName = org.apache.jena.util.SplitIRI.localname(iri);
        varContainer.addMerge(new VariableDef("G", objName));

        iri = m.listStatements(null, RDF.type, m.getOntClass(m.expandPrefix(":trace")))
                .nextOptional().map(Statement::getSubject).map(Resource::getURI)
                .orElseThrow();
        objName = org.apache.jena.util.SplitIRI.localname(iri);
        varContainer.addMerge(new VariableDef("T", objName));

        iri = m.listStatements(
                null,
                        m.getOntProperty(m.expandPrefix(":student_next_latest")),
                        (RDFNode)null
                )
                .nextOptional().map(Statement::getObject).map((t) -> t.asResource().getURI())
                .orElseThrow();
        objName = org.apache.jena.util.SplitIRI.localname(iri);
        varContainer.addMerge(new VariableDef("A", objName));


        return Collections.singletonList(new DecisionTreeReasonerBackend.DomainFact(situationModel));
    }

    @NotNull
    private its.model.definition.Domain getDTSituationModel(Model model) {
        // @see also: questionToDomainModel
        its.model.definition.Domain situationModel = domainSolvingModel.getDomain().getDomain().copy();
        DomainRDFFiller.fillDomain(
            situationModel,
                model,
            Collections.singleton(DomainRDFFiller.Option.NARY_RELATIONSHIPS_OLD_COMPAT),
            "http://vstu.ru/poas/code#" /*null*/
        );
        situationModel.validateAndThrowInvalid();
        return situationModel;
    }


    // ############

//    @Override
//    public Collection<Fact> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
//
//        Checkpointer ch = new Checkpointer(log);
//
//        // get question
//        QuestionEntity question = answerObjects.get(0).getQuestion();
//
//        ch.hit("responseToFacts: question obtained");
//
//        // proxy to super method
//        var responseFacts = super.responseToFacts(questionDomainType, responses, answerObjects);
//
//        ch.hit("responseToFacts: response facts obtained");
//
//        if (!responseFacts.isEmpty())
//        {
//            // call Jena to extend the prepared facts.
//
//            Backend backend = new JenaBackend();
//
//            JenaFactList fl = JenaFactList.fromBackendFacts(question.getStatementFacts());
//            fl.addFromModel(this.getSchemaForSolving());
//
//            ch.hit("responseToFacts: schema obtained");
//
//            Collection<Fact> newFacts = backend.judge(
//                    new ArrayList<>(this.getQuestionNegativeLaws(question.getQuestionDomainType(), null)),
//                    this.processQuestionFactsForBackendJudge(fl, responses, responseFacts),
//                    Fact.entitiesToFacts(question.getSolutionFacts()),
//                    responseFacts,
//                    new ReasoningOptions(
//                            false,
//                            this.getViolationVerbs(question.getQuestionDomainType(), question.getStatementFacts()),
//                            question.getQuestionName())
//            );
//            ch.hit("responseToFacts: call to Jena finished");
//            ch.since_start("responseToFacts: finished in");
//
//            // ...
//            return newFacts;
//        }
//
//        return new ArrayList<>();
//    }


    @Override
    public InterpretSentenceResult interpretSentence(Collection<Fact> violations) {
        List<ViolationEntity> mistakes = DecisionTreeReasonerBackend.reasonerOutputFactsToViolations(
            violations.stream().toList()
        );

        InterpretSentenceResult result = new InterpretSentenceResult();
        result.violations = mistakes;
        result.correctlyAppliedLaws = new ArrayList<>();
        result.isAnswerCorrect = mistakes.isEmpty();

        ProcessSolutionResult processResult = processSolution(violations);
        result.CountCorrectOptions = processResult.CountCorrectOptions;
        result.IterationsLeft = processResult.IterationsLeft;
        return result;
    }

    @Override
    public List<HyperText> makeExplanations(List<Fact> reasonerOutputFacts, Language lang) {
        return DecisionTreeReasonerBackend.makeExplanations(reasonerOutputFacts, lang);
    }

    @Override
    public List<HyperText> makeExplanation(List<ViolationEntity> mistakes, FeedbackType feedbackType, Language lang) {
        ArrayList<HyperText> result = new ArrayList<>();
        for (ViolationEntity mistake : mistakes) {
            result.add(makeSingleExplanation(mistake, feedbackType, lang));
        }
        return result;
    }

    private HyperText makeSingleExplanation(ViolationEntity mistake, FeedbackType feedbackType, Language lang) {
        return new HyperText("WRONG");
    }


    protected static OntModel modelToOntModel(Model model) {
        OntModel ontModel = ModelFactory.createOntologyModel(OWL_MEM);
        ontModel.add(model);
        return ontModel;
    }

    @Override
    public ProcessSolutionResult processSolution(Collection<Fact> solution) {
        its.model.definition.Domain domain = solution.stream()
                .filter(it -> it instanceof DecisionTreeReasonerBackend.DomainFact)
                .findFirst()
                .map((Fact t) -> ((DecisionTreeReasonerBackend.DomainFact)t).getDomain())
                .orElse(null);

        assert domain != null;
        OntModel ontModel = ModelFactory.createOntologyModel(OWL_MEM);
        Model schema = getVocabulary().getModel();
        ontModel.add(schema);
        Model model = DomainRDFWriter.saveDomain(domain,
            "http://vstu.ru/poas/code#",
            Collections.singleton(DomainRDFWriter.Option.NARY_RELATIONSHIPS_OLD_COMPAT)
        );
        ontModel.add(model);


        return processSolution(ontModel);
    }

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        val lastCorrectInteraction = Optional.ofNullable(q.getQuestionData().getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().isEmpty()) // select only interactions without mistakes
                .reduce((first, second) -> second);
        val lastCorrectInteractionAnswers = lastCorrectInteraction
                .flatMap(i -> Optional.ofNullable(i.getResponses())).stream()
                .flatMap(Collection::stream)
                // In Ordering Question, we need left answer objects only.
                .map(ResponseEntity::getLeftAnswerObject)
                .collect(Collectors.toList());

        return getNextCorrectAnswer(q, lastCorrectInteractionAnswers);
    }

    /**
     * @param q question
     * @return solution & statement facts as single model
     */
    private Model getSolutionModelOfQuestion(Question q) {
        // find next consequent (using solved facts)
        JenaFactList fl = JenaFactList.fromBackendFacts(q.getSolutionFacts());
        fl.addBackendFacts(q.getStatementFacts());
        return fl.getModel();
    }

    @Nullable
    protected CorrectAnswer getNextCorrectAnswer(Question q, @Nullable List<AnswerObjectEntity> correctTraceAnswersObjects) {
        return getNextCorrectAnswer(q, correctTraceAnswersObjects, modelToOntModel(getSolutionModelOfQuestion(q)));
    }

    @Override
    public Set<String> possibleViolations(Question q, List<ResponseEntity> completedSteps) {
        return possibleViolationsByStep(q,completedSteps)
                .stream()
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    @Override
    public Set<Set<String>> possibleViolationsByStep(Question q, List<ResponseEntity> completedSteps) {

        // use existing solution steps if given
        List<AnswerObjectEntity> correctTraceAnswersObjects = new ArrayList<>();

        if (completedSteps != null) {
            // extract answerObjects from given responses
            correctTraceAnswersObjects.addAll(completedSteps.stream().map(ResponseEntity::getLeftAnswerObject).collect(Collectors.toList()));
        }

        HashMap<String, Set<String>> map = new HashMap<>();

        // Construct remaining trace virtually, step by step
        while (true) {
            CorrectAnswer currentAct = getNextCorrectAnswer(q, correctTraceAnswersObjects);
            if (currentAct == null)
                break;

            // grow our virtual trace
            correctTraceAnswersObjects.add(currentAct.answers.get(0).getLeft());

            // find violations possible on this step
            final Set<String> possibleViolations = possibleMistakesByLaw(currentAct.lawName);

            final ArrayList<String> possibleViolationsSorted = new ArrayList<>(possibleViolations);
            Collections.sort(possibleViolationsSorted);
            String violSetKey = String.join(";", possibleViolationsSorted);

            // save unique sets of violations
            map.putIfAbsent(violSetKey, possibleViolations);
        }

        return new HashSet<>(map.values());
    }

    @Override
    public List<Question> getQuestionTemplates() {
        if (QUESTIONS == null) {
            QUESTIONS = readQuestions(this.getClass().getClassLoader().getResourceAsStream(QUESTIONS_CONFIG_PATH));
        }
        return QUESTIONS;
    }

    @Override
    public String getMessage(String message_text, Language preferred_language) {

        return localizationService.getMessage(MESSAGE_PREFIX + message_text, Language.getLocale(preferred_language));
    }

    /** fill in the blanks using StringSubstitutor class
     * @param s pattern of StringSubstitutor
     * @param placeholders argument map for pattern
     * @return
     */
    private static String replaceInString(String s, Map<String, String> placeholders) {
        // Build StringSubstitutor
        StringSubstitutor stringSubstitutor = new StringSubstitutor(placeholders);
        stringSubstitutor.setEnableUndefinedVariableException(true);

        // Replace in message
        try {
            return stringSubstitutor.replace(s);
        }
        catch (IllegalArgumentException exception) {
            return exception.getMessage() + " - template: " + s + " - placeholders: " + (placeholders.entrySet().stream()).map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
        }
    }


    private static void _test_Substitutor() {

        // Build map
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("animal", "quick brown fox");
        valuesMap.put("target", "lazy dog");
        valuesMap.put("name", "loop");
        String templateString = "The ${animal} jumped over the ${target} ${undefined.number:-1234567890} times.";

        // Build StringSubstitutor
        StringSubstitutor stringSubstitutor = new StringSubstitutor(valuesMap);
        stringSubstitutor.setEnableUndefinedVariableException(true);

        // Replace
        String resolvedString = stringSubstitutor.replace(templateString);
        log.debug(resolvedString);

        // change the map
        valuesMap.put("target", "pink pig");

        // Replace again
        resolvedString = stringSubstitutor.replace(templateString);
        log.debug(resolvedString);

        MessageFormat form = new MessageFormat("The disk \"{1}\" contains {0,choice,0#no files|1#one file|1<{0,number,integer} files}.");
//        double[] filelimits = {0,1,2};
//        String[] filepart = {"no files","one file","{0,number} files"};
//        ChoiceFormat fileform = new ChoiceFormat(filelimits, filepart);
//        form.setFormatByArgumentIndex(0, fileform);

        int fileCount = 12;
        String diskName = "MyDisk";
        Object[] testArgs = {(long) fileCount, diskName};

        log.debug(form.format(testArgs));

    }

    private static class AnswerDomainInfo {
        private final String domainInfo;
        private String phase;
        private String exId;
        private String traceActHypertext = null;

        public AnswerDomainInfo(String domainInfo) {
            this.domainInfo = domainInfo;
        }

        public String getPhase() {
            return phase;
        }

        public String getExId() {
            return exId;
        }

        public String getTraceActHypertext() {
            return traceActHypertext;
        }

        public AnswerDomainInfo invoke() {
            String[] actInfo = domainInfo.split(":");
            if (actInfo.length >= 2) {
                phase = actInfo[0];
                exId = actInfo[1];
            }
            if (actInfo.length >= 3) {
                traceActHypertext = actInfo[2];
            }
            return this;
        }
    }

    public static void main(String[] args) {
        if (true) {
            _test_Substitutor();
        } else {
            ControlFlowStatementsDTDomain d = ApplicationContextProvider.getApplicationContext().getBean(ControlFlowStatementsDTDomain.class);
            d.getQuestionTemplates();
            getVocabulary().classDescendants("Erroneous");

            JenaFactList fl = new JenaFactList(getVocabulary().getModel());
            fl.addBackendFacts(QUESTIONS.get(0).getStatementFacts());
        }
    }

}
