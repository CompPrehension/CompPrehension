package org.vstu.compprehension.models.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import its.model.DomainSolvingModel;
import its.model.definition.VariableDef;
import its.model.definition.rdf.DomainRDFFiller;
import its.model.definition.rdf.DomainRDFWriter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.text.StringSubstitutor;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.utils.ApplicationContextProvider;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.retainLeafOntClasses;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.testSubClassOfTransitive;

@Log4j2
public class ControlFlowStatementsDTDomain extends ControlFlowStatementsDomain {
    public static final String LOCALE_KEY_MARK = "!{locale:";
    static final String RESOURCES_LOCATION = "org/vstu/compprehension/models/businesslogic/domains/";
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String EXECUTION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderActsSupplementary";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
    static final String LAWS_CONFIG_PATH = RESOURCES_LOCATION + "control-flow-statements-domain-laws.json";
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
    private static List<String> fieldPropertiesCache = null;


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
            AbstractRdfStorage qMetaStorage
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

    /* **
     * Jena is used to SOLVE this domain's questions
     ** /
    @Override
    public String getSolvingBackendId(){
        return JenaBackend.BackendId;
    }  // */

    /**
     * Decision Tree Reasoner is used to JUDGE this domain's questions.
     */
    @Override
    public String getJudgingBackendId(/* TODO: pass question type ??*/){
        return DecisionTreeReasonerBackend.BACKEND_ID;
    }

    public String getDBShortName(){
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

    /**
     * Off. Don't use.
     * Make negative laws that name each possible error & set bitflags for each law as defined within the vocabulary.
     */
//    @Override
    protected void ___loadNegativeLawsFromVocabulary() {
        // add negative laws that name each possible error
        // Note: get flags and write to law object.

        val voc = getVocabulary();
        val model = voc.getModel();
        val prop_has_bitflags = model.getProperty("law_bitflags");
        String ns = model.getNsPrefixURI("");

        for (String errClass : voc.classDescendants("Erroneous")) {
//            val classNode = model.getResource(ns + errClass).as(OntClass.class);
//            val classNode = model.createClass(ns + errClass);
            val classNode = model.getOntClass(ns + errClass);
            val law = new NegativeLaw(errClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);

            // fill data: bitFlags
            val bitFlagStmt = classNode.getProperty(prop_has_bitflags);
            if (bitFlagStmt != null) {
                law.setBitflags(bitFlagStmt.getInt());
            }
            // fill data: impliesLaws
            law.setImpliesLaws(new ArrayList<>());
            for (val superClass : classNode.listSuperClasses(/*direct:*/true).toSet()) {
                String name = superClass.getLocalName();
                if (name.equals(errClass)) {
                    continue;
                }
                law.getImpliesLaws().add(name);
            }

            negativeLaws.put(errClass, law);
        }
        // law.setBitmask() will be called afterwards outside this method.
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

//    @Override
    public Collection<Fact> _OFF__processQuestionFactsForBackendSolve(Collection<Fact> questionFacts) {

        // USE Jena: prepare solved facts since DT reasoner does nothing for solve()
        Backend backend = new JenaBackend();

        Collection<Fact> newFacts = backend.solve(
                new ArrayList<>(this.getQuestionPositiveLaws(EXECUTION_ORDER_QUESTION_TYPE /*<Fixme: only one type of question is supported!*/, null)),
                questionFacts,
                new ReasoningOptions(
                        false,
                        this.getViolationVerbs(EXECUTION_ORDER_QUESTION_TYPE, List.of()),
                        null)
        );

        return newFacts;

//        var model = JenaFactList.fromFacts(newFacts).getModel();
//
//        its.model.definition.Domain situationModel = getDTSituationModel(model);
//
//        return Collections.singletonList(new DecisionTreeReasonerBackend.DomainFact(situationModel));
    }

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

    private static List<String> getFieldProperties() {
        if (fieldPropertiesCache == null)
            fieldPropertiesCache = getVocabulary().propertyDescendants("string_placeholder");
        return fieldPropertiesCache;
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


    /** Append specific facts to `factsList` */
    private void appendActFacts(Collection<Fact> factsList, int id, String actIRI, String ontoClass, String executesId, Integer studentIndex, String prevActIRI, String inTrace, Boolean exprValue, boolean isLatest) {
        factsList.add(new Fact(
                "owl:NamedIndividual", actIRI,
                "rdf:type",
                "owl:Class", ontoClass
        ));
        factsList.add(new Fact(
                "owl:NamedIndividual", actIRI,
                "id",
                "xsd:int", String.valueOf(id)
        ));
        if (executesId != null) {
            factsList.add(new Fact(
                    "owl:NamedIndividual", actIRI,
                    "executes_id",
                    "xsd:int", String.valueOf(executesId)
            ));
        }
        if (studentIndex != null) {
            factsList.add(new Fact(
                    "owl:NamedIndividual", actIRI,
                    "student_index",
                    "xsd:int", String.valueOf(studentIndex)
            ));
        }
        if (prevActIRI != null) {
            factsList.add(new Fact(
                    "owl:NamedIndividual",
                    prevActIRI,                     "student_next",
                    "owl:NamedIndividual", actIRI
            ));
            if (isLatest) {
                factsList.add(new Fact(
                        "owl:NamedIndividual",
                        prevActIRI, "student_next_latest",
                        "owl:NamedIndividual", actIRI
                ));
            }
        }
        factsList.add(new Fact(
                "owl:NamedIndividual", actIRI,
                "in_trace",
                "owl:NamedIndividual", inTrace
        ));
        if (exprValue != null) {
            factsList.add(new Fact(
                    "owl:NamedIndividual", actIRI,
                    "expr_value",
                    "xsd:boolean", exprValue.toString()
            ));
        }
    }

//    @Override
    public InterpretSentenceResult _old__interpretSentence(Collection<Fact> violations) {
        InterpretSentenceResult result = new InterpretSentenceResult();
        List<ViolationEntity> mistakes = new ArrayList<>();
        HashSet<String> mistakeTypes = new HashSet<>();

        OntModel model = factsAndSchemaToOntModel(violations);

//        ///
//        try {
//            model.write(new FileOutputStream("c:/temp/interpret.n3"), Lang.NTRIPLES.getName());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        OntClass Erroneous = model.getOntClass(model.expandPrefix(":Erroneous"));
        AnnotationProperty atom_action = model.getAnnotationProperty(model.expandPrefix(":atom_action"));
        Property stmt_name = model.getProperty(model.expandPrefix(":stmt_name"));
        Property executes = model.getProperty(model.expandPrefix(":executes"));
        Property boundary_of = model.getProperty(model.expandPrefix(":boundary_of"));
        Property begin_of    = model.getProperty(model.expandPrefix(":begin_of"));
        Property end_of      = model.getProperty(model.expandPrefix(":end_of"));
        Property halt_of     = model.getProperty(model.expandPrefix(":halt_of"));
        Property wrong_next_act = model.getProperty(model.expandPrefix(":wrong_next_act"));
        Property reason = model.getProperty(model.expandPrefix(":reason"));
        OntClass OwlClass = model.getOntClass(OWL.Class.getURI());
        Literal True = model.createTypedLiteral(true);

//        Set<? extends OntResource> instSet = Erroneous.listInstances().toSet();
        Set<RDFNode> instSet = model.listObjectsOfProperty(wrong_next_act).toSet();
        for (RDFNode inst : instSet) {
            // inst = instSet.next();

            // find the most specific error class
            if (inst instanceof Resource) {
                Resource act_individual = inst.asResource();


                // filter classNodes of act instance
                List<OntClass> classes = new ArrayList<>();
                List<RDFNode> classNodes = model.listObjectsOfProperty(inst.asResource(), RDF.type).toList();
                classNodes.forEach(rdfNode -> {
                    if (rdfNode instanceof Resource && rdfNode.asResource().hasProperty(RDF.type, OwlClass))
                        classes.add(model.createClass(rdfNode.asResource().getURI()));
                });

                List<OntClass> errorOntClasses = retainLeafOntClasses(
                        // act_individual.listOntClasses(true).toList()
                        classes
                );
                //     properties_to_extract = ("id", "name", onto.precursor, onto.cause, onto.should_be,
                //     onto.should_be_before, onto.should_be_after, onto.context_should_be, onto.text_line, )

                Individual action = act_individual.getPropertyResourceValue(executes).getPropertyResourceValue(boundary_of).as(Individual.class);

                String act_stmt_name = action.getPropertyValue(stmt_name).asLiteral().getString();

                // extract ALL field_* facts, no matter what law they belong to.
                List<Statement> actLinks = model.listStatements(act_individual, null, (String) null).toList();
                HashMap<String, String> placeholders = new HashMap<>();
                for (Statement statement : actLinks) {
                    String verb = statement.getPredicate().getLocalName();
                    if (getFieldProperties().contains(verb)) {
                        String fieldName = verb.replaceAll("field_", "");
                        String value;
                        if (!fieldName.endsWith("_bound")) {
                            // object is just an ordinal string
                            value = statement.getString();
                        } else {
                            // process bound instance ...
                            // statement.object is boundary, so we can retrieve action name & phase

                            // cut "_bound" suffix
                            fieldName = fieldName.substring(0, fieldName.length() - "_bound".length());
                            // add 'phased-' prefix
                            fieldName = "phased-" + fieldName;

                            Individual bound = statement.getObject().asResource().as(Individual.class);
                            Individual action_ = bound.getProperty(boundary_of).getObject().as(Individual.class);
                            // value = bound.boundary_of.stmt_name
                            value = action_.getPropertyValue(stmt_name).asLiteral().getString();

                            // bound's action does not have 'atom_action'=true annotation
                            if (action_.listOntClasses(false).toSet().stream()
                                    .filter(c -> model.listStatements(c, atom_action, True).hasNext())
                                    .findAny()
                                    .isEmpty()
                            ) {
                                // find phase from bound's relation
                                String phase_str = "";  // templated version since we don't know the locale now
                                if (model.listStatements(bound, begin_of, action_).hasNext()) {
                                    phase_str = "!{locale:phase.begin_of}" + " ";  //// getMessage("phase.begin_of", );
                                } else if (model.listStatements(bound, end_of, action_).hasNext()
                                        || model.listStatements(bound, halt_of, action_).hasNext()) {
                                    phase_str = "!{locale:phase.end_of}" + " ";
                                }
                                // prepend prefix
                                value = phase_str + value;
                                // change case of description intro (that differs when used with phase in Russian)
                                value = value.replaceFirst("!\\{locale:text\\.", "!{locale:text.phased-");
                            }
                        }

                        // add to placeholders ...
                        value = "«" + value + "»";
                        if (placeholders.containsKey(fieldName)) {
                            String prevData = placeholders.get(fieldName);
                            // if not in previous data
                            if (!prevData.equals(value) && !prevData.contains(value))
                                // append to previous data
                                value = prevData + ", " + value;
                            //// System.out.println((":: WARNING :: retrieving field_* facts: clash at key '" + fieldName + "'.\n\tValues:\n\told: " + placeholders.get(fieldName) + "\n\tnew: " + value));
                        }
                        placeholders.put(fieldName, value);
                    }
                }

                ///
                //// System.out.println("\nPlaceholders:");
                //// System.out.println(placeholders);

                for (OntClass errClass : errorOntClasses) {
                    // filter out not-error classes
                    if (!testSubClassOfTransitive(errClass, Erroneous)) {
                        continue;
                    }

                    String mistakeType = errClass.getLocalName();

                    // skip mistake of the type that's already here
                    if (mistakeTypes.contains(mistakeType)) {
                        continue;
                    }

                    mistakeTypes.add(mistakeType);

                    ///
                    //// System.out.println("<>- Mistake for action " + act_stmt_name + ": " + mistakeType);


                    ViolationEntity violationEntity = new ViolationEntity();
                    violationEntity.setLawName(mistakeType);

                    List<ExplanationTemplateInfoEntity> templates = new ArrayList<>();
                    placeholders.forEach((name, value) -> {
                        ExplanationTemplateInfoEntity explT = new ExplanationTemplateInfoEntity();
                        explT.setFieldName(name);
                        explT.setValue(value);
                        explT.setViolation(violationEntity);
                        templates.add(explT);
                    });
                    violationEntity.setExplanationTemplateInfo(templates);

                    violationEntity.setViolationFacts(new ArrayList<>(Arrays.asList(
                            new BackendFactEntity("owl:NamedIndividual", act_individual.getLocalName(),
                                    "stmt_name",
                                    "string", act_stmt_name) //,
                            // TODO: add more (mistake-specific?) facts
                    )));

                    if (mistakeType.toLowerCase().contains("neighbour")) {
                        // prepend
                        mistakes.add(0, violationEntity);
                    } else {
                        // append
                        mistakes.add(violationEntity);
                    }
                }
            } else {
                ///
                log.debug("Cannot treat obj as Resource: {}", inst);
            }
        }

        result.violations = mistakes;
        mistakeTypes.clear();

        // reason - наследник связи consequent (вычисляется ризонером для latest акта)
        List<RDFNode> reasons = model.listObjectsOfProperty(reason).toList();
        ArrayList<String> correctlyAppliedLaws = new ArrayList<>();
        for (RDFNode reasonClass : reasons) {
            correctlyAppliedLaws.add(reasonClass.asResource().getLocalName());
        }
        // add possible but not taken mistakes
        correctlyAppliedLaws.addAll(notHappenedMistakes(correctlyAppliedLaws, violations));
        result.correctlyAppliedLaws = correctlyAppliedLaws;

        ProcessSolutionResult processResult = processSolution(violations);
        result.CountCorrectOptions = processResult.CountCorrectOptions;
        result.IterationsLeft = processResult.IterationsLeft; // + (mistakes.isEmpty() ? 0 : 1);
        return result;
    }

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

    private static OntModel factsAndSchemaToOntModel(Collection<Fact> facts) {
        Model schema = getVocabulary().getModel();
        JenaFactList fl = JenaFactList.fromFacts(facts);
        return modelToOntModel(schema.union(fl.getModel()));
    }

    private static Collection<Fact> modelToFacts(Model factsModel) {
        return new JenaFactList(factsModel);
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
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0) // select only interactions without mistakes
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

//    public CorrectAnswer getRemainingCorrectAnswers(Question q) {
//
//        return null;
//    }

    public static List<Question> readQuestions(InputStream inputStream) {
        List<Question> res = new ArrayList<>();

        Gson gson = getQuestionGson();

        Question[] questions = gson.fromJson(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                Question[].class);

        Collections.addAll(res, questions);
        return res;
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

    /** format pattern using MessageFormat class
     * @param pattern pattern of MessageFormat
     * @param arguments arguments for pattern
     * @return formatted string
     */
    private static String formatTemplate(String pattern, Object... arguments) {
        // from: https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html
        return (new MessageFormat(pattern)).format(arguments, new StringBuffer(), null).toString();
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

    private HashMap<String, Long> _getViolationsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(16);
        name2bit.put("DuplicateOfAct", 0x1L);
        name2bit.put("ElseBranchAfterTrueCondition", 0x2L);
        name2bit.put("NoAlternativeEndAfterBranch", 0x4L);
        name2bit.put("NoBranchWhenConditionIsTrue", 0x8L);
        name2bit.put("NoFirstCondition", 0x10L);
        name2bit.put("SequenceFinishedTooEarly", 0x20L);
        name2bit.put("TooEarlyInSequence", 0x40L);
        name2bit.put("BranchOfFalseCondition", 0x80L);
        name2bit.put("LastConditionIsFalseButNoElse", 0x100L);
        name2bit.put("LastFalseNoEnd", 0x200L);
        name2bit.put("LoopStartIsNotCondition", 0x400L);
        name2bit.put("NoLoopEndAfterFailedCondition", 0x800L);
        name2bit.put("NoConditionAfterIteration", 0x1000L);
        name2bit.put("NoIterationAfterSuccessfulCondition", 0x2000L);
        name2bit.put("LoopStartIsNotIteration", 0x4000L);
        return name2bit;
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
