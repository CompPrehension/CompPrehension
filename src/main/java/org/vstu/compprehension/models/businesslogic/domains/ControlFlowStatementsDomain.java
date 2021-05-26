package org.vstu.compprehension.models.businesslogic.domains;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.opentest4j.AssertionFailedError;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.utils.HyperText;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.getLeafOntClasses;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.testSubClassOfTransitive;

@Component
public class ControlFlowStatementsDomain extends Domain {
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String EXECUTION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderActsSupplementary";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
//    static final String LAWS_CONFIG_PATH = "file:c:/D/Work/YDev/CompPr/c_owl/jena/domain_laws.json";
    static final String LAWS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-laws.json";

    /// TODO: copy the dictionary to CompPrehension repository
    static final String VOCAB_SCHEMA_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-schema.rdf";
    private static DomainVocabulary VOCAB = null;

    static final String QUESTIONS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-questions.json";
    static List<Question> QUESTIONS;

    public ControlFlowStatementsDomain() {
        super();
        name = "ControlFlowStatementsDomain";
        fillConcepts();
        readLaws(this.getClass().getClassLoader().getResourceAsStream(LAWS_CONFIG_PATH));
    }

    private static void initVocab() {
        if (VOCAB == null) {
            VOCAB = new DomainVocabulary(VOCAB_SCHEMA_PATH);
        }
    }

    private void fillConcepts() {
        concepts = new ArrayList<>();
        initVocab();
        concepts.addAll(VOCAB.readConcepts());
    }

    private void readLaws(InputStream inputStream) {
        Objects.requireNonNull(inputStream);
        positiveLaws = new ArrayList<>();
        negativeLaws = new ArrayList<>();

        RuntimeTypeAdapterFactory<Law> runtimeTypeAdapterFactory =
                RuntimeTypeAdapterFactory
                        .of(Law.class, "positive")
                        .registerSubtype(PositiveLaw.class, "true")
                        .registerSubtype(NegativeLaw.class, "false");
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

        Law[] lawForms = gson.fromJson(
                new InputStreamReader(inputStream),
                Law[].class);

        for (Law lawForm : lawForms) {
            if (lawForm.isPositiveLaw()) {
                positiveLaws.add((PositiveLaw) lawForm);
            } else {
                negativeLaws.add((NegativeLaw) lawForm);
            }
        }
    }


    @Override
    public void update() {
    }

    @Override
    public List<HyperText> getFullSolutionTrace(Question question) {
        ///
        System.out.println("\t\tGetting the trace ...");

        ArrayList<HyperText> result = new ArrayList<>();

        HashMap<String, Integer> exprName2ExecTime = new HashMap<>();

        String qType = question.getQuestionData().getQuestionDomainType();
        if (qType.equals(EXECUTION_ORDER_QUESTION_TYPE) || qType.equals("Type" + EXECUTION_ORDER_QUESTION_TYPE)) {
            List<InteractionEntity> interactions = question.getQuestionData().getInteractions();
            if (interactions != null && !interactions.isEmpty()) {
                InteractionEntity interaction = null;
                for (InteractionEntity i : Lists.reverse(interactions)) {
                    interaction = i;
                    if (i.getViolations().isEmpty()) {
                        break;
                    }
                }
                assertNotNull(interaction, "Reverse() nulled the list?");
                for (ResponseEntity response : interaction.getResponses()) {
//                    if (!interaction.getViolations().isEmpty())
//                        // skip erroneous interaction
//                        continue;

                    AnswerObjectEntity answerObj = response.getLeftAnswerObject();
                    String domainInfo = answerObj.getDomainInfo();
                    AnswerDomainInfo info = new AnswerDomainInfo(domainInfo).invoke();
                    String line;
                    if (info.getTraceActHypertext() != null) {
                        line = info.getTraceActHypertext();
                    } else {
                        line = "(" + domainInfo + ")";
                    }
                    // add expression value if necessary
                    if (answerObj.getConcept().equals("expr")) {
                        String phase = info.getPhase();
                        String exId = info.getExId();
                        String exprName = getExpressionNameById(question.getQuestionData(), Integer.parseInt(exId));
                        int execTime = 1 + exprName2ExecTime.getOrDefault(exprName, 0);
                        int value = getValueForExpression(question.getQuestionData(), exprName, execTime);
                        exprName2ExecTime.put(exprName, execTime);

                        String valueStr = (value == 1) ? "true" : "false";
                        // TODO: add HTML styling
                        line = line + " -> " + valueStr;
                    }
                    result.add(new HyperText(line));
                    ///
                    System.out.println(result.get(result.size() - 1).getText());
                }
            }
        } else {
            ///
            result.addAll(Arrays.asList(
                    new HyperText("debugging trace line #1 for unknown question Type" + question.getQuestionData().getQuestionDomainType()),
                    new HyperText("trace <b>line</b> #2"),
                    new HyperText("trace <i>line</i> #3")
            ));
        }

        if (result.isEmpty()) {
            // add initial tip
            result.add(new HyperText("Solve this question by clicking " +
                    "<img src=\"https://icons.bootstrap-4.ru/assets/icons/play-fill.svg\" alt=\"Play\" width=\"22\">" +
                    " play and " +
                    "<img src=\"https://icons.bootstrap-4.ru/assets/icons/stop-fill.svg\" alt=\"Stop\" width=\"20\">" +
                    " stop buttons"));
        }

        ///
        System.out.println("\t\tObtained the trace. Last line is:");
        System.out.println(result.get(result.size() - 1).getText());

        return result;
    }

    @Override
    public ExerciseForm getExerciseForm() {
        return null;
    }

    @Override
    public ExerciseEntity processExerciseForm(ExerciseForm ef) {
        return null;
    }

    @Override
    public Question makeQuestion(QuestionRequest questionRequest, List<Tag> tags, Language userLanguage) {
        // Prepare concept name sets ...
        HashSet<String> conceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getTargetConcepts()) {
            conceptNames.add(concept.getName());
        }
        HashSet<String> allowedConceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getAllowedConcepts()) {
            allowedConceptNames.add(concept.getName());
        }
        HashSet<String> deniedConceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getDeniedConcepts()) {
            deniedConceptNames.add(concept.getName());
        }
        deniedConceptNames.add("supplementary");

        Question res = findQuestion(tags, conceptNames, allowedConceptNames, deniedConceptNames, new HashSet<>());
        if (res == null) {
            // get anything. TODO: make it input-dependent
            res = QUESTIONS.get(0);
        }
        return makeQuestionCopy(res, questionRequest.getExerciseAttempt());
    }

//    public List<BackendFactEntity> getBackendFacts(List<String> expression) {
//        List<BackendFactEntity> facts = new ArrayList<>();
//        int index = 0;
//        for (String token : expression) {
//            index++;
//            for (int step = 0; step <= expression.size(); ++step) {
//                String name = getName(step, index);
//                facts.add(new BackendFactEntity(name, "rdf:type", "owl:NamedIndividual"));
//                facts.add(new BackendFactEntity("owl:NamedIndividual", name, "index", "xsd:int", String.valueOf(index)));
//                facts.add(new BackendFactEntity("owl:NamedIndividual", name, "step", "xsd:int", String.valueOf(step)));
//            }
//            facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "text", "xsd:string", token));
//        }
//        facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "last", "xsd:boolean", "true"));
//        return facts;
//    }

    static List<BackendFactEntity> schemaFactsCache = null;

    static List<BackendFactEntity> getSchemaFacts() {
        if (schemaFactsCache == null) {
            schemaFactsCache = modelToFacts(VOCAB.getModel());
        }
        return schemaFactsCache;
    }

    Question makeQuestionCopy(Question q, ExerciseAttemptEntity exerciseAttemptEntity) {
        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showTrace(true)
                .multipleSelectionEnabled(true)
                .orderNumberOptions(new OrderQuestionOptionsEntity.OrderNumberOptions("/", OrderQuestionOptionsEntity.OrderNumberPosition.NONE, null))
                .build();

        QuestionOptionsEntity matchingQuestionOptions = MatchingQuestionOptionsEntity.builder()
                .requireContext(false)
                .displayMode(MatchingQuestionOptionsEntity.DisplayMode.COMBOBOX)
                .build();

        QuestionOptionsEntity multiChoiceQuestionOptions = QuestionOptionsEntity.builder()
                .requireContext(false)
                .build();

        QuestionEntity entity = new QuestionEntity();
        List<AnswerObjectEntity> answerObjectEntities = new ArrayList<>();
        for (AnswerObjectEntity answerObjectEntity : q.getAnswerObjects()) {
            AnswerObjectEntity newAnswerObjectEntity = new AnswerObjectEntity();
            newAnswerObjectEntity.setAnswerId(answerObjectEntity.getAnswerId());
            newAnswerObjectEntity.setConcept(answerObjectEntity.getConcept());
            newAnswerObjectEntity.setDomainInfo(answerObjectEntity.getDomainInfo());
            newAnswerObjectEntity.setHyperText(answerObjectEntity.getHyperText());
            newAnswerObjectEntity.setQuestion(entity);
            newAnswerObjectEntity.setRightCol(answerObjectEntity.isRightCol());
            newAnswerObjectEntity.setResponsesLeft(new ArrayList<>());
            newAnswerObjectEntity.setResponsesRight(new ArrayList<>());
            answerObjectEntities.add(newAnswerObjectEntity);
        }
        entity.setAnswerObjects(answerObjectEntities);
        entity.setAreAnswersRequireContext(true);
        entity.setExerciseAttempt(exerciseAttemptEntity);
        entity.setQuestionDomainType(q.getQuestionDomainType());

        // statement facts are already prepared in the Question's JSON
        List<BackendFactEntity> facts = new ArrayList<>(q.getStatementFacts());
        // add schema facts!
        facts.addAll(getSchemaFacts());
        entity.setStatementFacts(facts);
        entity.setQuestionType(q.getQuestionType());


        switch (q.getQuestionType()) {
            case ORDER:
                entity.setQuestionText((q.getQuestionText().getText()));
                entity.setOptions(orderQuestionOptions);
                return new Ordering(entity);
            case MATCHING:
                entity.setQuestionText((q.getQuestionText().getText()));
                entity.setOptions(matchingQuestionOptions);
                return new Matching(entity);
            case MULTI_CHOICE:
                entity.setQuestionText((q.getQuestionText().getText()));
                entity.setOptions(multiChoiceQuestionOptions);
                return new MultiChoice(entity);
            default:
                throw new UnsupportedOperationException("Unknown type in ControlFlowStatementsDomain::makeQuestion: " + q.getQuestionType());
        }
    }

    /**
     * Generate explanation of violations
     *
     * @param violations   list of student violations
     * @param feedbackType TODO: use feedbackType or delete it
     * @return explanation for each violation in random order
     */
    @Override
    public List<HyperText> makeExplanation(List<ViolationEntity> violations, FeedbackType feedbackType) {

        violations.forEach(System.out::println);

        if (violations.isEmpty())
            return new ArrayList<>();
        else {
            ArrayList<HyperText> explanation = new ArrayList<>();
            violations.forEach(ve -> explanation.add(new HyperText(ve.getLawName() + " violated... ")));
            return explanation;
        }
    }

    // filter positive laws by question type and tags
    @Override
    public List<PositiveLaw> getQuestionPositiveLaws(String domainQuestionType, List<Tag> tags) {
        /// debug OFF
        if (false && domainQuestionType.equals(EXECUTION_ORDER_QUESTION_TYPE) || domainQuestionType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
            List<PositiveLaw> positiveLaws = new ArrayList<>();
            for (PositiveLaw law : getPositiveLaws()) {
                boolean needLaw = true;
                for (Tag tag : law.getTags()) {  // also, include a law without tags
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
                if (needLaw) {
                    positiveLaws.add(law);
                }
            }
            return positiveLaws;
        }
        // return new ArrayList<>(Collections.emptyList());
        return getPositiveLaws();
    }

    public List<NegativeLaw> getQuestionNegativeLaws(String domainQuestionType, List<Tag> tags) {
        /// debug OFF
        if (false && domainQuestionType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            List<NegativeLaw> negativeLaws = new ArrayList<>();
            for (NegativeLaw law : getNegativeLaws()) {
                boolean needLaw = true;
                //filter by tags after separation
                for (Tag tag : law.getTags()) {  // also, include a law without tags
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
                if (needLaw) {
                    negativeLaws.add(law);
                }
            }
            return negativeLaws;
        }
        // return new ArrayList<>(Collections.emptyList());
        return getNegativeLaws();
    }

    @Override
    public List<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        // proxy to static method
        return getSolutionVerbsStatic(questionDomainType, statementFacts);
    }

    public static List<String> getSolutionVerbsStatic(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "rdf:type",
                    "id",
                    "boundary_of",
                    "begin_of",
                    "end_of",
                    "consequent",
                    "has_upcoming",
                    "normal_consequent",
                    "always_consequent",
                    "on_true_consequent",
                    "on_false_consequent",
                    "entry_point",
                    "stmt_name",
                    "parent_of",
                    "branches_item",
                    "next",
                    "cond",
                    "body",
                    "body_item",
                    "index",
                    "executes_id",
                    "executes"
            ));
//        } else if (questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
//            return new ArrayList<>(Arrays.asList(
//                    "wrong_type"
//            ));
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return getViolationVerbsStatic(questionDomainType, statementFacts);
    }

    public static List<String> getViolationVerbsStatic(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            Set<String> verbs = new HashSet<>(Arrays.asList(
                    // - "*whole_model*" //,
                    "rdf:type",
                    "rdfs:subClassOf",
                    "rdfs:subPropertyOf",
                     "id",
                    // "name",
                    "next_act",
                    "student_next",
                    "wrong_next_act",
                    "corresponding_end",
                    "student_corresponding_end",
                    "parent_of",
                    "student_parent_of",
                    "executes_id",
                    "executes",
                    "precursor",
                    "cause",
                    "has_causing_condition",
                    "should_be",
                    "should_be_before",
                    "should_be_after",
                    "context_should_be"
            ));
            // add solution verbs too!
            verbs.addAll(getSolutionVerbsStatic(questionDomainType, statementFacts));
            return new ArrayList<>(verbs);
//        } else if (questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
//            return new ArrayList<>(Arrays.asList(
//                    "wrong_type"
//            ));
        }
        return new ArrayList<>();
    }

    @Override
    public List<BackendFactEntity> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
        // proxy to static method
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {

            // get question
            QuestionEntity q = answerObjects.get(0).getQuestion();

            // init result facts with solution facts
            List<BackendFactEntity> result = new ArrayList<>();
            result.addAll(getSchemaFacts());
            result.addAll(q.getSolutionFacts());

            // trace object
            String trace = "comp-ph-trace";
            appendActFacts(result, 0, trace, "trace", null, 0, null, trace, null);

            result.add(new BackendFactEntity(
                    "owl:NamedIndividual", trace,
                    "index",
                    "xsd:int", "0"
            ));
//            make_triple(trace_obj, onto.exec_time, 0)  # set to 0 so next is 1
            result.add(new BackendFactEntity(
                    "owl:NamedIndividual", trace,
                    "depth",
                    "xsd:int", "0"
            ));
//            make_triple(trace_obj, onto.index, 0)
//            make_triple(trace_obj, onto.student_index, 0)
//            make_triple(trace_obj, onto.depth, 0)  # set to 0 so next is 1
//            make_triple(trace_obj, onto.in_trace, trace_obj)  # each act

            // iterate responses and make acts
            int student_index = 0;
            int trace_index = 0;
            int maxId = 100;
            HashMap<String, MutablePair<String, Integer>> id2exprName = new HashMap<>();
            String prevActIRI = trace;
            for (ResponseEntity response : responses) {
                if (response.getInteraction() != null && !response.getInteraction().getViolations().isEmpty())
                    // skip erroneous responses
                    continue;

                trace_index ++;
                String domainInfo = response.getLeftAnswerObject().getDomainInfo();
                ///
                System.out.println("Adding act from response: " + domainInfo);

                AnswerDomainInfo answerDomainInfo = new AnswerDomainInfo(domainInfo).invoke();
                String phase = answerDomainInfo.getPhase();
                String exId = answerDomainInfo.getExId();
//              int exId = Integer.parseInt(actInfo[1]);

                String act_iri_t = exId + "_n" + trace_index;

                if (phase.equals("started") || phase.equals("performed")) {
                    String act_iri = "b_" + act_iri_t;
                    appendActFacts(result, ++maxId, act_iri, "act_begin", exId, ++student_index, prevActIRI, trace, null);
                    prevActIRI = act_iri;
                } if (phase.equals("finished") || phase.equals("performed")) {
                    Boolean exprValue = null;
                    if (phase.equals("performed")) {
                        String exprName = null;
                        Integer execCount = null;
                        if (id2exprName.containsKey(exId)) {
                            // get the entry from the map
                            MutablePair<String, Integer> pair = id2exprName.get(exId);
                            exprName = pair.getLeft();
                            execCount = pair.getRight();
                            // we encounter the expr one more time
                            ++execCount;
                            pair.setRight(execCount);
                        } else {
                            exprName = getExpressionNameById(q, Integer.parseInt(exId));
                            if (exprName != null) {
                                execCount = 1;
                                // add the entry to the map
                                id2exprName.put(exId, new MutablePair<>(exprName, execCount));
                            }
                        }
                        if (exprName != null) {
                            exprValue = (1 == getValueForExpression(q, exprName, execCount));
                        }
                    }
                    String act_iri = "e_" + act_iri_t;
                    appendActFacts(result, ++maxId, act_iri, "act_end", exId, ++student_index, prevActIRI, trace, exprValue);
                    prevActIRI = act_iri;
                }
            }

            return result;
        }
        return new ArrayList<>();
    }


    /** Append specific facts to `factsList` */
    private void appendActFacts(List<BackendFactEntity> factsList, int id, String actIRI, String ontoClass, String executesId, Integer studentIndex, String prevActIRI, String inTrace, Boolean exprValue) {
        factsList.add(new BackendFactEntity(
                "owl:NamedIndividual", actIRI,
                "rdf:type",
                "owl:Class", ontoClass
        ));
        factsList.add(new BackendFactEntity(
                "owl:NamedIndividual", actIRI,
                "id",
                "xsd:int", String.valueOf(id)
        ));
        if (executesId != null) {
            factsList.add(new BackendFactEntity(
                    "owl:NamedIndividual", actIRI,
                    "executes_id",
                    "xsd:int", String.valueOf(executesId)
            ));
        }
        if (studentIndex != null) {
            factsList.add(new BackendFactEntity(
                    "owl:NamedIndividual", actIRI,
                    "student_index",
                    "xsd:int", String.valueOf(studentIndex)
            ));
        }
        if (prevActIRI != null) {
            factsList.add(new BackendFactEntity(
                    "owl:NamedIndividual",
                    prevActIRI,                     "student_next",
                    "owl:NamedIndividual", actIRI
            ));
        }
        factsList.add(new BackendFactEntity(
                "owl:NamedIndividual", actIRI,
                "in_trace",
                "owl:NamedIndividual", inTrace
        ));
        if (exprValue != null) {
            factsList.add(new BackendFactEntity(
                    "owl:NamedIndividual", actIRI,
                    "expr_value",
                    "xsd:boolean", exprValue.toString()
            ));
        }
    }

    @Override
    public InterpretSentenceResult interpretSentence(List<BackendFactEntity> violations) {
        InterpretSentenceResult result = new InterpretSentenceResult();
        List<ViolationEntity> mistakes = new ArrayList<>();

        OntModel model = factsToOntModel(violations);

        OntClass Erroneous = model.getOntClass(model.expandPrefix(":Erroneous"));
        Property stmt_name = model.getProperty(model.expandPrefix(":stmt_name"));
        Property executes = model.getProperty(model.expandPrefix(":executes"));
        Property boundary_of = model.getProperty(model.expandPrefix(":boundary_of"));
        Property wrong_next_act = model.getProperty(model.expandPrefix(":wrong_next_act"));

//        Set<? extends OntResource> instSet = Erroneous.listInstances().toSet();
        Set<RDFNode> instSet = model.listObjectsOfProperty(wrong_next_act).toSet();
        for (RDFNode inst : instSet) {
            // inst = instSet.next();

            // find the most specific error class
            if (inst instanceof Resource) {
                Resource act_individual = inst.asResource();


                // filter classNodes
                OntClass OwlClass = model.getOntClass(OWL.Class.getURI());
                List<OntClass> classes = new ArrayList<>();
                List<RDFNode> classNodes = model.listObjectsOfProperty(inst.asResource(), RDF.type).toList();
                classNodes.forEach(rdfNode -> {
                    if (rdfNode instanceof Resource && rdfNode.asResource().hasProperty(RDF.type, OwlClass))
                        classes.add(model.createClass(rdfNode.asResource().getURI()));
                });

                List<OntClass> errorOntClasses = getLeafOntClasses(
                        // act_individual.listOntClasses(true).toList()
                        classes
                );
                //     properties_to_extract = ("id", "name", onto.precursor, onto.cause, onto.should_be,
                //     onto.should_be_before, onto.should_be_after, onto.context_should_be, onto.text_line, )

                Individual action = act_individual.getPropertyResourceValue(executes).getPropertyResourceValue(boundary_of).as(Individual.class);

                String act_stmt_name = action.getPropertyValue(stmt_name).asLiteral().getString();

                for (OntClass errClass : errorOntClasses) {
                    // filter out not-error classes
                    if (!testSubClassOfTransitive(errClass, Erroneous))
                        continue;

                    ///
                    System.out.println("<>- Mistake for action " + act_stmt_name + ": " + errClass.getLocalName());


                    ViolationEntity violationEntity = new ViolationEntity();
                    violationEntity.setLawName(errClass.getLocalName());

                    ExplanationTemplateInfoEntity explT = new ExplanationTemplateInfoEntity();
                    explT.setValue("it was happened..!");
                    violationEntity.setExplanationTemplateInfo(Arrays.asList(
                            explT
                    ));

                    violationEntity.setViolationFacts(new ArrayList<>(Arrays.asList(
                            new BackendFactEntity("owl:NamedIndividual", act_individual.getLocalName(),
                                    "stmt_name",
                                    "string", act_stmt_name) //,
                            // TODO: add more (mistake-specific?) facts
                    )));
                    mistakes.add(violationEntity);
                }
            } else {
                ///
                System.out.println("Cannot treat obj as Resource: " + inst);
            }
        }


        result.violations = mistakes;
        result.correctlyAppliedLaws = new ArrayList<>(); // TODO: проследовать по связи consequent

        ProcessSolutionResult processResult = processSolution(violations);
        result.CountCorrectOptions = processResult.CountCorrectOptions;
        result.IterationsLeft = processResult.IterationsLeft; // + (mistakes.isEmpty() ? 0 : 1);
        return result;
    }

    @Override
    public Question makeSupplementaryQuestion(QuestionEntity question, ViolationEntity violation) {
        throw new NotImplementedException();
    }

    @Override
    public InterpretSentenceResult judgeSupplementaryQuestion(Question question, AnswerObjectEntity answer) {
        throw new NotImplementedException();
    }

    private static OntModel factsToOntModel(List<BackendFactEntity> backendFacts) {
        JenaBackend jback = new JenaBackend();

        Model schema = VOCAB.getModel();
        String base = schema.getNsPrefixURI("");
        // strip # at right
        base = base.replaceAll("#+$", "");
        jback.createOntology(base);

        OntModel model = jback.getModel();

        // fill with schema
        model.add(schema);

        jback.addFacts(backendFacts);

        return model;
    }

    private static List<BackendFactEntity> modelToFacts(Model factsModel) {
        JenaBackend jback = new JenaBackend();

        Model schema = VOCAB.getModel();
        String base = schema.getNsPrefixURI("");
        // strip # at right
        base = base.replaceAll("#+$", "");
        jback.createOntology(base);

        // fill with schema
        OntModel model = jback.getModel();
        model.add(factsModel);

        return jback.getFacts(getViolationVerbsStatic(EXECUTION_ORDER_QUESTION_TYPE, null));
    }

    @Override
    public ProcessSolutionResult processSolution(List<BackendFactEntity> solution) {
        OntModel model = factsToOntModel(solution);

        return processSolution(model);
    }

    /** receive solution as model */
    private ProcessSolutionResult processSolution(OntModel model) {
        InterpretSentenceResult result = new InterpretSentenceResult();
        // always one correct answer
        result.CountCorrectOptions = 1;

        // retrieving full solution path ...
        int pathLen = 0;
        try {
            // 1) find last act of partial trace (the one having no student_next prop) ...
            Individual lastAct = null;
            OntProperty student_next = model.getOntProperty(model.expandPrefix(":student_next"));
            var acts = model.getOntClass(model.expandPrefix(":act")).listInstances(false);
            while (lastAct == null && acts.hasNext()) {
                Individual act = (Individual) acts.next();
                if (!act.hasProperty(student_next)) {
                    lastAct = act;
                }
            }
            // if last act is wrong, roll back to correct one
            OntClass Erroneous = model.getOntClass(model.expandPrefix(":Erroneous"));
            while (lastAct != null && lastAct.hasOntClass(Erroneous)) {
                List<Resource> prevActAsList =
                        model.listResourcesWithProperty(student_next, lastAct).toList();
                Resource prevAct = prevActAsList.isEmpty() ? null : prevActAsList.get(0);
                try {
                    lastAct = (Individual) prevAct;
                } catch (ClassCastException exception) {
                    ///
                    System.out.println("Warning: Cannot cast " + prevAct.getURI() + " to Individual ...");
                    lastAct = null;
                }
            }
            Individual actionInd = null;
            if (lastAct != null) {
                OntProperty executes = model.createOntProperty(model.expandPrefix(":executes"));
                OntProperty boundary_of = model.createOntProperty(model.expandPrefix(":boundary_of"));
                // get action of last act
                Resource bound = lastAct.getPropertyResourceValue(executes);
                if (bound != null) {
                    actionInd = bound.getPropertyResourceValue(boundary_of).as(Individual.class);
                }
            }
            if (lastAct == null || actionInd == null) {
                ObjectProperty entry_point = model.getObjectProperty(model.expandPrefix(":entry_point"));
                List<RDFNode> entryAsList = model.listObjectsOfProperty(entry_point).toList();
                // retrieve entry point of algorithm

                assertFalse(entryAsList.isEmpty(), "Missing entry point in the algorithm!");

                actionInd = entryAsList.get(0).as(Individual.class);
            }

            /// assert actionInd != null;  // did not get last act correctly ?
            assertNotNull(actionInd, "did last act retrieved correctly?");

            // 2) find length of shortest path over algorithm to the end ...
            // get shortcuts to properties
            OntProperty boundary_of = model.getOntProperty(model.expandPrefix(":boundary_of"));
            OntProperty begin_of = model.getOntProperty(model.expandPrefix(":begin_of"));
            OntProperty end_of = model.getOntProperty(model.expandPrefix(":end_of"));
            OntProperty on_false_consequent = model.getOntProperty(model.expandPrefix(":on_false_consequent"));
            OntProperty consequent = model.getOntProperty(model.expandPrefix(":consequent"));
            // get boundary of the initial action
            List<Resource> bounds = model.listSubjectsWithProperty(begin_of, actionInd).toList(); // actionInd
            // .getPropertyResourceValue(boundary_of);
            assertFalse(bounds.isEmpty(), "no bounds found for entry point!");
            Individual bound = bounds.get(0).as(Individual.class);

            // boolean endOfPath = false;
            while (bound != null) {
                Individual nextBound = null;
                // move along on_false_consequent, or along "consequent" if absent
                if (bound.hasProperty(on_false_consequent)) {
                    ++pathLen;
                    // move to next bound
                    nextBound = bound.getPropertyResourceValue(on_false_consequent).as(Individual.class);
                } else if (bound.hasProperty(consequent)) {
                    ++pathLen;
                    // move to next bound
                    nextBound = bound.getPropertyResourceValue(consequent).as(Individual.class);
                    // check if simple action (stmt or expr)
                    if (bound.hasProperty(begin_of) && nextBound.hasProperty(end_of)) {
                        // ignore this link as simple statements show as a whole
                        --pathLen;
                    }
                }
                bound = nextBound;
            }
        } catch (AssertionFailedError error) {
            pathLen = 99;
            System.out.println("WARN: processSolution(): cannot find entry_point, fallback to: pathLen = " + pathLen);
        }

        result.IterationsLeft = pathLen;
        return result;
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
                .map(r -> Pair.of(r.getLeftAnswerObject(), r.getRightAnswerObject()))
                .collect(Collectors.toList());

//        List<InteractionEntity> interactions = q.getQuestionData().getInteractions();

        AnswerObjectEntity lastAnswer =
                lastCorrectInteractionAnswers.get(lastCorrectInteractionAnswers.size() - 1).getLeft();

        String domainInfo = lastAnswer.getDomainInfo();
        AnswerDomainInfo answerDomainInfo = new AnswerDomainInfo(domainInfo).invoke();
        String phase = answerDomainInfo.getPhase();
        String exId = answerDomainInfo.getExId();


        val solution = q.getSolutionFacts();
        assertNotNull(solution, "Call solve question before getAnyNextCorrectAnswer");

        OntModel model = factsToOntModel(solution);

        // find any consequent of last correct act ...
        // get shortcuts to properties
        OntProperty consequent = model.getOntProperty(model.expandPrefix(":consequent"));
        OntProperty boundary_of = model.getOntProperty(model.expandPrefix(":boundary_of"));
        OntProperty begin_of = model.getOntProperty(model.expandPrefix(":begin_of"));
        OntProperty end_of = model.getOntProperty(model.expandPrefix(":end_of"));
        OntProperty id = model.getOntProperty(model.expandPrefix(":id"));

        Individual actionFrom = model.listResourcesWithProperty(id, Integer.parseInt(exId)).nextResource().as(Individual.class);

        Individual boundFrom = model.listResourcesWithProperty(
                phase.equalsIgnoreCase("started")? begin_of : end_of,
                actionFrom).nextResource().as(Individual.class);

        Individual boundTo = boundFrom.getPropertyResourceValue(consequent).as(Individual.class);

        Individual actionTo = boundTo.getPropertyResourceValue(boundary_of).as(Individual.class);


        String idTo =  actionTo.getPropertyValue(id).asLiteral().getLexicalForm();
        String phaseTo =  boundTo.hasProperty(begin_of) ? "started" : "finished";
        // check if actionTo is stmt/expr
        if (phaseTo.equals("started") && (
                actionTo.hasOntClass(model.getOntClass(model.expandPrefix(":stmt"))) || actionTo.hasOntClass(model.getOntClass(model.expandPrefix(":expr")))
                )) {
            phaseTo = "performed";
        }

        // next correct answer found: question answer domain info
        String qaInfoPrefix = idTo + ":" + phaseTo;

        ///
        System.out.println("next correct answer found: " + qaInfoPrefix);

        // find question answer
        ArrayList<Pair<AnswerObjectEntity, AnswerObjectEntity>> answers = new ArrayList<>();  // lastCorrectInteractionAnswers);
        for (AnswerObjectEntity answer : q.getAnswerObjects()) {
            if (answer.getDomainInfo().startsWith(qaInfoPrefix)) {
                answers.add(Pair.of(answer, answer));
            }
        }

        // make result
        CorrectAnswer correctAnswer = new CorrectAnswer();
        correctAnswer.question = q.getQuestionData();
        correctAnswer.answers = answers;
        correctAnswer.lawName = "No correct law yet, using flow graph";  // answerImpl.lawName;
        correctAnswer.explanation = new HyperText("explanation: TODO"); // getCorrectExplanation(answerImpl.lawName);
        return correctAnswer;
    }

    private List<Question> readQuestions(InputStream inputStream) {
        List<Question> res = new ArrayList<>();

        RuntimeTypeAdapterFactory<Question> runtimeTypeAdapterFactory =
                RuntimeTypeAdapterFactory
                        .of(Question.class, "questionType")
                        .registerSubtype(Ordering.class, "ORDERING")
                        .registerSubtype(SingleChoice.class, "SINGLE_CHOICE")
                        .registerSubtype(MultiChoice.class, "MULTI_CHOICE")
                        .registerSubtype(Matching.class, "MATCHING");
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

        Question[] questions = gson.fromJson(
                new InputStreamReader(inputStream),
                Question[].class);

        Collections.addAll(res, questions);
        return res;
    }

    @Override
    protected List<Question> getQuestionTemplates() {
        if (QUESTIONS == null) {
            QUESTIONS = readQuestions(this.getClass().getClassLoader().getResourceAsStream(QUESTIONS_CONFIG_PATH));
        }
        return QUESTIONS;
    }

    /** return stmt_name or `null` if not an `expr` */
    private String getExpressionNameById(QuestionEntity question, int actionId) {
        String instance = null;
        for (BackendFactEntity fact : question.getStatementFacts()) {
            if (fact.getVerb().equals("id") && Integer.parseInt(fact.getObject()) == actionId) {
                instance = fact.getSubject();
            }
        }
        String stmt_name = null;
        boolean isEXpr = false;
        for (BackendFactEntity fact : question.getStatementFacts()) {
            if (fact.getSubject().equals(instance)) {
                if (fact.getVerb().equals("rdf:type") && fact.getObject().equals("expr")) {
                    isEXpr = true;
                }
                if (fact.getVerb().equals("stmt_name") && fact.getObject().equals("expr")) {
                    stmt_name = fact.getObject();
                }
            }
        }
        if (isEXpr)
            return stmt_name;
        return null;
    }

    public int getValueForExpression(QuestionEntity question, String expressionName, int executionTime) {
        for (BackendFactEntity fact : question.getStatementFacts()) {
            if (fact.getSubject().equals(expressionName) && fact.getVerb().equals("not-for-reasoner:expr_values") && fact.getObjectType().equals("List<boolean>")) {
                String values = fact.getObject();
                String[] tokens = values.split(",");
                if (executionTime <= tokens.length) {
                    String token = tokens[executionTime - 1];
                    return Integer.parseInt(token);
                }
                // else (out-of-range): go return the default
                break;
            }
        }

        // default is false
        return 0;
    }


    public static void main(String[] args) {
        ControlFlowStatementsDomain d = new ControlFlowStatementsDomain();
        d.getQuestionTemplates();
        VOCAB.classDescendants("Erroneous");


        JenaBackend jBack = new JenaBackend();
        jBack.createOntology();
        // jBack.getModel().add(VOCAB.getModel());
        jBack.addFacts(modelToFacts(VOCAB.getModel()));
        jBack.addFacts(QUESTIONS.get(0).getStatementFacts());
//        jBack.debug_dump_model("question");
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
}
