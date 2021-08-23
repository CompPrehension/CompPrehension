package org.vstu.compprehension.models.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
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
import org.vstu.compprehension.utils.LocalizationMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.getLeafOntClasses;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.testSubClassOfTransitive;
import static org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph.factsListDeepCopy;

@Component
public class ControlFlowStatementsDomain extends Domain {
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String EXECUTION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderActsSupplementary";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
//    static final String LAWS_CONFIG_PATH = "file:c:/D/Work/YDev/CompPr/c_owl/jena/domain_laws.json";
    static final String LAWS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-laws.json";
    static final String MESSAGES_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-messages.txt";

    // dictionary
    static final String VOCAB_SCHEMA_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-schema.rdf";
    private static DomainVocabulary VOCAB = null;

    static final String QUESTIONS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-questions.json";
    static List<Question> QUESTIONS;
    private static List<String> reasonPropertiesCache = null;
    private static List<String> fieldPropertiesCache = null;

    private static HashMap<String, LocalizationMap> MESSAGES = null;

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

        // add empty laws that name each possible error
        for (String errClass : VOCAB.classDescendants("Erroneous")) {
            negativeLaws.add(new NegativeLaw(errClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null));
        }

    }

    @Override
    public List<Concept> getLawConcepts(Law law) {
        return null;
    }

    @Override
    public void update() {
    }

    @Override
    public List<HyperText> getFullSolutionTrace(Question question) {
        /// System.out.println("\t\tGetting the trace ...");

        ArrayList<HyperText> result = new ArrayList<>();

        HashMap<String, Integer> exprName2ExecTime = new HashMap<>();

        String qType = question.getQuestionData().getQuestionDomainType();
        if (qType.equals(EXECUTION_ORDER_QUESTION_TYPE) || qType.equals("Type" + EXECUTION_ORDER_QUESTION_TYPE)) {
            for (ResponseEntity response : responsesForTrace(question.getQuestionData(), true)) {
                boolean responseIsWrong = ! response.getInteraction().getViolations().isEmpty();

                AnswerObjectEntity answerObj = response.getLeftAnswerObject();
                String domainInfo = answerObj.getDomainInfo();
                AnswerDomainInfo info = new AnswerDomainInfo(domainInfo).invoke();
                String line;
                if (info.getTraceActHypertext() != null) {
                    line = info.getTraceActHypertext();
                } else {
                    line = "(" + domainInfo + ")";
                }

                int execTime = 1 + exprName2ExecTime.getOrDefault(domainInfo, 0);
                line = line + " " + nthTimeByN(execTime);
                exprName2ExecTime.put(domainInfo, execTime);

                // add expression value if necessary
                if (answerObj.getConcept().equals("expr")) {
                    String valueStr;
                    if (responseIsWrong) {
                        /// valueStr = "not evaluated";
                        valueStr = "не вычислено";
                    } else {
                        String phase = info.getPhase();
                        String exId = info.getExId();
                        String exprName = getExpressionNameById(question.getQuestionData(), Integer.parseInt(exId));
                        int value = getValueForExpression(question.getQuestionData(), exprName, execTime);

                        /// valueStr = (value == 1) ? "true" : "false";
                        valueStr = (value == 1) ? "истина" : "ложь";
                    }
                    // add HTML styling
                    line = line + " -> " + htmlStyled("atom", valueStr);
                }

                // check if this line is wrong
                if (responseIsWrong) {
                    // add background color
                    line = htmlStyled("warning", line);
                }

                result.add(new HyperText(line));
                ///
//                System.out.println(result.get(result.size() - 1).getText());
            }
        } else {
            ///
            result.addAll(Arrays.asList(
                    new HyperText("debugging trace line #1 for unknown question Type" + question.getQuestionData().getQuestionDomainType()),
                    new HyperText("trace <b>line</b> #2"),
                    new HyperText("trace <i>line</i> #3")
            ));
        }

//        if (result.isEmpty()) {
//            // add initial tip
//            result.add(new HyperText("Solve this question by clicking " +
//                    "<img src=\"https://icons.bootstrap-4.ru/assets/icons/play-fill.svg\" alt=\"Play\" width=\"22\">" +
//                    " play and " +
//                    "<img src=\"https://icons.bootstrap-4.ru/assets/icons/stop-fill.svg\" alt=\"Stop\" width=\"20\">" +
//                    " stop buttons"));
//        }

        ///
//        System.out.println("\t\tObtained the trace. Last line is:");
//        System.out.println(result.get(result.size() - 1).getText());

        return result;
    }

    private List<ResponseEntity> responsesForTrace(QuestionEntity q, boolean allowLastIncorrect) {

        ArrayList<ResponseEntity> responses = new ArrayList<ResponseEntity>();

        List<InteractionEntity> interactions = q.getInteractions();

        if (interactions == null || interactions.isEmpty()) {
            return responses; // empty so far
            // early exit: no further checks for emptiness
        }

        for (InteractionEntity i : interactions) {
            if (i.getViolations().isEmpty()) {
                // this is correct interaction, so it appends one more correct response, - get it
                List<ResponseEntity> currentResponses = i.getResponses();
                if (currentResponses != null && !currentResponses.isEmpty()) {
                    ResponseEntity lastResponse = currentResponses.get(currentResponses.size() - 1);

                    responses.add(lastResponse);
                }
            }
        }

        /*  prev variant
        InteractionEntity lastCorrectInteraction = null;
        for (InteractionEntity i : Lists.reverse(interactions)) {
            if (i.getViolations().isEmpty()) {
                lastCorrectInteraction = i;
                break;
            }
        }
        if (lastCorrectInteraction != null) {
            responses.addAll(lastCorrectInteraction.getResponses());
        }
        */

        if (allowLastIncorrect) {
            InteractionEntity lastInteraction = interactions.get(interactions.size() - 1);
            // lastInteraction is wrong
            if (!lastInteraction.getViolations().isEmpty()) {
                List<ResponseEntity> lastResponses = lastInteraction.getResponses();
                if (lastResponses != null && !lastResponses.isEmpty()) {
                    ResponseEntity lastResponse = lastResponses.get(lastResponses.size() - 1);

                    responses.add(lastResponse);
                }
            }
        }
        return responses;
    }

    private String htmlStyled(String styleClass, String text) {
        return "<span class=\"" + styleClass + "\">" + text + "</span>";
    }

    private String nthTimeByN(int n) {
        String suffix, time = "time";
        /// switch (n) {
        ///     case 1: suffix = "st"; break;
        ///     case 2: suffix = "nd"; break;
        ///     case 3: suffix = "rd"; break;
        ///     default: suffix = "th";
        /// }
        suffix = "-й";
        time = "раз";
        return htmlStyled("number", n + suffix) + " " + time;
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

        HashSet<String> deniedQuestions = new HashSet<>();
        if (questionRequest.getExerciseAttempt() != null && questionRequest.getExerciseAttempt().getQuestions() != null) {
            for (QuestionEntity q : questionRequest.getExerciseAttempt().getQuestions()) {
                deniedQuestions.add(q.getQuestionName());
            }
        }
        Question res = findQuestion(tags, conceptNames, deniedConceptNames, lawNames, deniedLawNames, deniedQuestions);
        if (res == null) {
            // get anything. TODO: make it input-dependent
            // get (a random) index
            int index = new Random().nextInt(QUESTIONS.size());
            res = QUESTIONS.get(index);
        }
        return makeQuestionCopy(res, questionRequest.getExerciseAttempt(), userLanguage);
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

    static List<BackendFactEntity> getSchemaFacts(boolean deepCopy) {
        List<BackendFactEntity> schemaFacts = getSchemaFacts();
        if (deepCopy) {
            return factsListDeepCopy(schemaFacts);
        }

        return schemaFacts;
    }

    protected Question makeQuestionCopy(Question q, ExerciseAttemptEntity exerciseAttemptEntity, Language userLanguage) {
        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showSupplementaryQuestions(false)
                .showTrace(true)
                .multipleSelectionEnabled(true)
                //.requireAllAnswers(false)
                .orderNumberOptions(new OrderQuestionOptionsEntity.OrderNumberOptions("/", OrderQuestionOptionsEntity.OrderNumberPosition.NONE, null))
                .build();

        QuestionOptionsEntity matchingQuestionOptions = MatchingQuestionOptionsEntity.builder()
                .requireContext(false)
                // .showSupplementaryQuestions(false)
                .displayMode(MatchingQuestionOptionsEntity.DisplayMode.COMBOBOX)
                .build();

        QuestionOptionsEntity multiChoiceQuestionOptions = QuestionOptionsEntity.builder()
                .requireContext(false)
                .build();

        QuestionEntity entity = new QuestionEntity();
        List<AnswerObjectEntity> answerObjectEntities = new ArrayList<>();
        for (AnswerObjectEntity answerObjectEntity : q.getAnswerObjects()) {
            AnswerObjectEntity newAnswerObjectEntity = new AnswerObjectEntity();
            newAnswerObjectEntity.setQuestion(entity);
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
        entity.setQuestionName(q.getQuestionName());

        // add schema facts!
        List<BackendFactEntity> facts = new ArrayList<>(getSchemaFacts(true));
        // statement facts are already prepared in the Question's JSON
        facts.addAll(factsListDeepCopy(q.getStatementFacts()));
        entity.setStatementFacts(facts);
        entity.setQuestionType(q.getQuestionType());


        switch (q.getQuestionType()) {
            case ORDER:
                val baseQuestionText = getFrontMessages().get("ORDER_question_prompt").get(userLanguage);
                entity.setQuestionText(baseQuestionText + q.getQuestionText().getText());
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
     * @param lang user preferred language
     * @return explanation for each violation in random order
     */
    @Override
    public List<HyperText> makeExplanation(List<ViolationEntity> violations, FeedbackType feedbackType, Language lang) {

//        if (feedbackType != FeedbackType.EXPLANATION) {
//            //
//        }

        /// violations.forEach(System.out::println);

        if (violations.isEmpty())
            return new ArrayList<>();
        else {
            // rearrange mistakes ..?

            ArrayList<HyperText> explanation = new ArrayList<>();
            violations.forEach(ve -> explanation.add(makeExplanation(ve, feedbackType, lang)));
            return explanation;
        }
    }

    public HyperText makeExplanation(ViolationEntity violation, FeedbackType feedbackType, Language userLang) {
        String lawName = violation.getLawName();
        String msg = getFrontMessages().get(lawName).get(userLang);

        if (msg == null) {
            return new HyperText("[Empty explanation] for " + lawName);
        }

        // fill placeholders
        for (ExplanationTemplateInfoEntity template : violation.getExplanationTemplateInfo()) {
            String pattern = "<" + template.getFieldName() + ">";
            if (!msg.contains(pattern)) {
                pattern = "<list-" + template.getFieldName() + ">";
            }
            String replacement = template.getValue();
            msg = msg.replaceAll(pattern, replacement);
        }

        return new HyperText(msg);
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
            Set<String> verbs = new HashSet<>(Arrays.asList(
//            return new ArrayList<>(Arrays.asList(
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
                    "executes",
                    "reason_kind",
                    "to_reason",
                    "from_reason"
            ));
            if (reasonPropertiesCache == null)
                reasonPropertiesCache = VOCAB.propertyDescendants("consequent");
            verbs.addAll(reasonPropertiesCache);
            verbs.addAll(getFieldProperties());
            return new ArrayList<>(verbs);
        }
        return new ArrayList<>();
    }

    private static List<String> getFieldProperties() {
        if (fieldPropertiesCache == null)
            fieldPropertiesCache = VOCAB.propertyDescendants("string_placeholder");
        return fieldPropertiesCache;
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
                    "student_next_latest",
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
                    "context_should_be",
                    "reason"
            ));
            // add solution verbs too!
            verbs.addAll(getSolutionVerbsStatic(questionDomainType, statementFacts));
            return new ArrayList<>(verbs);
        }
        return new ArrayList<>();
    }

    @Override
    public List<BackendFactEntity> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
        // proxy to static method
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {

            // get question
            QuestionEntity q = answerObjects.get(0).getQuestion();

            // obtain correct only responses (in different way!)
            List<ResponseEntity> responsesByQ = responsesForTrace(q, false);

            // append latest response to list of correct responses
            if (!responses.isEmpty()) {
                ResponseEntity latestResponse = responses.get(responses.size() - 1);
                responsesByQ.add(latestResponse);
            }

            // reassign responses to [[correct] + new]
            responses = responsesByQ;

            // init result facts with solution facts
            List<BackendFactEntity> result = new ArrayList<>();

            //// result.addAll(getSchemaFacts());
            //// result.addAll(q.getStatementFacts());
            //// result.addAll(q.getSolutionFacts());

            // find algorithm id
            String entryPointIri = null;
            // extract ID from local name (having format <id>_<name>)
            for (BackendFactEntity fact : q.getStatementFacts()) {
//                {
//                    "subjectType": "owl:NamedIndividual",
//                        "subject": "30_algorithm",
//                        "verb": "entry_point",
//                        "objectType": "owl:NamedIndividual",
//                        "object": "31_global_code"
//                },
                if (fact.getVerb().equals("entry_point")) {
                    entryPointIri = fact.getObject();  //// .split("_", 2)[0];
                    break;
                }
            }
            assertNotNull(entryPointIri, "No entry point declared among q.statementFacts");
            // trace object
            String trace = "comp-ph-trace";
            appendActFacts(result, 0, trace, "trace",
                    //// null,
                    entryPointIri.split("_", 2)[0],
                    0, null, trace, null, false);
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

            ResponseEntity latestResponse = null;
            if (!responses.isEmpty()) {
                latestResponse = responses.get(responses.size() - 1);
            }

            for (ResponseEntity response : responses) {
//                if (response.getInteraction() != null && !response.getInteraction().getViolations().isEmpty())
//                    // skip responses known to be  erroneous
//                    continue;
                boolean isLatest = (response == latestResponse);

                trace_index ++;
                AnswerObjectEntity ao = response.getLeftAnswerObject();
                String domainInfo = ao.getDomainInfo();
                ///
                /// System.out.println("Adding act from response: " + ao.getHyperText());

                AnswerDomainInfo answerDomainInfo = new AnswerDomainInfo(domainInfo).invoke();
                String phase = answerDomainInfo.getPhase();
                String exId = answerDomainInfo.getExId();
//              int exId = Integer.parseInt(actInfo[1]);

                String act_iri_t = exId + "_n" + trace_index;

                if (phase.equals("started") || phase.equals("performed")) {
                    String act_iri = "b_" + act_iri_t;
                    appendActFacts(result, ++maxId, act_iri, "act_begin", exId, ++student_index, prevActIRI, trace, null, isLatest);
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
                    appendActFacts(result, ++maxId, act_iri, "act_end", exId, ++student_index, prevActIRI, trace, exprValue, isLatest && !phase.equals("performed"));
                    prevActIRI = act_iri;
                }
            }

            return result;
        }
        return new ArrayList<>();
    }


    /** Append specific facts to `factsList` */
    private void appendActFacts(List<BackendFactEntity> factsList, int id, String actIRI, String ontoClass, String executesId, Integer studentIndex, String prevActIRI, String inTrace, Boolean exprValue, boolean isLatest) {
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
            if (isLatest) {
                factsList.add(new BackendFactEntity(
                        "owl:NamedIndividual",
                        prevActIRI, "student_next_latest",
                        "owl:NamedIndividual", actIRI
                ));
            }
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
        HashSet<String> mistakeTypes = new HashSet<>();

        OntModel model = factsToOntModel(violations);

//        ///
//        try {
//            model.write(new FileOutputStream("c:/temp/interpret.n3"), Lang.NTRIPLES.getName());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        OntClass Erroneous = model.getOntClass(model.expandPrefix(":Erroneous"));
        Property stmt_name = model.getProperty(model.expandPrefix(":stmt_name"));
        Property executes = model.getProperty(model.expandPrefix(":executes"));
        Property boundary_of = model.getProperty(model.expandPrefix(":boundary_of"));
        Property wrong_next_act = model.getProperty(model.expandPrefix(":wrong_next_act"));
        Property reason = model.getProperty(model.expandPrefix(":reason"));

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

                // extract ALL field_* facts, no matter what law they belong to.
                List<Statement> actLinks = model.listStatements(act_individual, null, (String) null).toList();
                HashMap<String, String> placeholders = new HashMap<>();
                for (Statement statement : actLinks) {
                    String verb = statement.getPredicate().getLocalName();
                    if (getFieldProperties().contains(verb)) {
                        String fieldName = verb.replaceAll("field_", "");
                        String value = statement.getString();
                        value = "\"" + value + "\"";
                        if (placeholders.containsKey(fieldName)) {
                            // append to previous data
                            value = placeholders.get(fieldName) + ", " + value;
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
                System.out.println("Cannot treat obj as Resource: " + inst);
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

    Set<String> notHappenedMistakes(List<String> correctLaws, List<BackendFactEntity> questionFacts) {
        HashSet<String> mistakeNames = new HashSet<>();
        // find out if context mistakes are applicable here
        for (BackendFactEntity f : questionFacts) {
            if (f.getVerb().equals("rdf:type") && (f.getObject().equals("alternative") || f.getObject().endsWith("loop"))) {
                mistakeNames.add("CorrespondingEndMismatched");
                mistakeNames.add("EndedDeeper");
                mistakeNames.add("EndedShallower");
                mistakeNames.add("WrongContext");
                mistakeNames.add("OneLevelShallower");
                break;
            }
        }
        // use heuristics to get possible mistakes
        for (String corrLaw : correctLaws) {
            switch (corrLaw) {
                case("SequenceBegin"):
                    mistakeNames.add("TooEarlyInSequence");
                    mistakeNames.add("SequenceFinishedTooEarly");
                    break;
                case("SequenceNext"):
                    mistakeNames.add("TooEarlyInSequence");
                    mistakeNames.add("TooLateInSequence");
                    mistakeNames.add("SequenceFinishedTooEarly");
                    mistakeNames.add("DuplicateOfAct");
                    break;
                case("SequenceEnd"):
                    mistakeNames.add("TooEarlyInSequence");
                    mistakeNames.add("SequenceFinishedNotInOrder");
                    break;
                case("AltBegin"):
                    mistakeNames.add("NoFirstCondition");
                    mistakeNames.add("BranchWithoutCondition");
                    break;
                case("AltBranchBegin"):
                    mistakeNames.add("BranchNotNextToCondition");
                    mistakeNames.add("ElseBranchNotNextToLastCondition");
                    mistakeNames.add("ElseBranchAfterTrueCondition");
                    mistakeNames.add("CondtionNotNextToPrevCondition");
                    mistakeNames.add("ConditionTooEarly");
                    mistakeNames.add("ConditionTooLate");
                    mistakeNames.add("DuplicateOfCondition");
                    mistakeNames.add("NoNextCondition");
                    mistakeNames.add("NoBranchWhenConditionIsTrue");
                    mistakeNames.add("AlternativeEndAfterTrueCondition");
                    break;
                case("NextAltCondition"):
                    mistakeNames.add("BranchNotNextToCondition");
                    mistakeNames.add("ElseBranchNotNextToLastCondition");
                    mistakeNames.add("CondtionNotNextToPrevCondition");
                    mistakeNames.add("ConditionTooEarly");
                    mistakeNames.add("ConditionTooLate");
                    mistakeNames.add("DuplicateOfCondition");
                    mistakeNames.add("NoNextCondition");
                    mistakeNames.add("BranchOfFalseCondition");
                    mistakeNames.add("BranchWithoutCondition");
                    break;
                case("AltEndAfterBranch"):
                    mistakeNames.add("ConditionAfterBranch");
                    mistakeNames.add("AnotherExtraBranch");
                    mistakeNames.add("NoAlternativeEndAfterBranch");
                    break;
                case("AltEndAllFalse"):
                    mistakeNames.add("BranchOfFalseCondition");
                    mistakeNames.add("LastFalseNoEnd");
                    break;
                case("AltElseBranchBegin"):
                    mistakeNames.add("LastConditionIsFalseButNoElse");
                    break;
                case("IterationBeginOnTrueCond"):
                    mistakeNames.add("NoIterationAfterSuccessfulCondition");
                    mistakeNames.add("LoopEndAfterSuccessfulCondition");
                    break;
                case("PreCondLoopBegin"):
                    mistakeNames.add("LoopStartIsNotCondition");
                    break;
                case("PostCondLoopBegin"):
                    mistakeNames.add("LoopStartIsNotIteration");
                    break;
                case("LoopEndOnFalseCond"):
                    mistakeNames.add("LoopContinuedAfterFailedCondition");
                    mistakeNames.add("IterationAfterFailedCondition");
                    break;
                case("LoopCondBeginAfterIteration"):
                    mistakeNames.add("NoConditionAfterIteration");
                    mistakeNames.add("NoConditionBetweenIterations");
                    break;
            }
        }
        return mistakeNames;
    }

    @Override
    public Question makeSupplementaryQuestion(QuestionEntity question, ViolationEntity violation, Language userLang) {
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
//            OntProperty student_next = model.getOntProperty(model.expandPrefix(":student_next"));
            OntProperty student_next_latest = model.getOntProperty(model.expandPrefix(":student_next_latest"));

            List<Statement> tripleLastInTraceAsList =
                    model.listStatements(null, student_next_latest, (RDFNode) null).toList();

            assertFalse(tripleLastInTraceAsList.isEmpty());
            Statement tripleLastInTrace = tripleLastInTraceAsList.get(0);

            Individual lastAct = tripleLastInTrace.getObject().as(Individual.class);

            // if last act is wrong, roll back to previous (the correct one)
            OntClass Erroneous = model.getOntClass(model.expandPrefix(":Erroneous"));
            if (lastAct.hasOntClass(Erroneous)) {
                lastAct = tripleLastInTrace.getSubject().as(Individual.class);
            }

            Individual actionInd = null;
            Resource boundRes = null;
            if (lastAct != null) {
                OntProperty executes = model.createOntProperty(model.expandPrefix(":executes"));
                OntProperty boundary_of = model.createOntProperty(model.expandPrefix(":boundary_of"));
                // get action of last act
                boundRes = lastAct.getPropertyResourceValue(executes);
                if (boundRes != null) {
                    actionInd = boundRes.getPropertyResourceValue(boundary_of).as(Individual.class);
                }
            }
            if (lastAct == null || actionInd == null) {
                // retrieve entry point of algorithm
                ObjectProperty entry_point = model.getObjectProperty(model.expandPrefix(":entry_point"));
                List<RDFNode> entryAsList = model.listObjectsOfProperty(entry_point).toList();

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
            Individual bound;
            if (boundRes == null) {
                List<Resource> bounds = model.listSubjectsWithProperty(begin_of, actionInd).toList(); // actionInd
                // .getPropertyResourceValue(boundary_of);
                assertFalse(bounds.isEmpty(), "no bounds found for entry point!");
                boundRes = bounds.get(0);
            }
            bound = boundRes.as(Individual.class);

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
            // the last transition (to PROGRAM ENDED) exists in the graph but not shown in GUI - skip it
            --pathLen;
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
        String phase;
        String exId;

        if (lastCorrectInteractionAnswers.isEmpty()) {
            // get first act in trace
            List<AnswerObjectEntity> answers = q.getQuestionData().getAnswerObjects();
            AnswerObjectEntity firstAnswer = answers.get(0);
            String domainInfo = firstAnswer.getDomainInfo();
            AnswerDomainInfo answerDomainInfo = new AnswerDomainInfo(domainInfo).invoke();

            phase = answerDomainInfo.getPhase();
            exId = answerDomainInfo.getExId();

        } else {
            AnswerObjectEntity lastAnswer =
                    lastCorrectInteractionAnswers.get(lastCorrectInteractionAnswers.size() - 1).getLeft();

            String domainInfo = lastAnswer.getDomainInfo();
            AnswerDomainInfo answerDomainInfo = new AnswerDomainInfo(domainInfo).invoke();
            phase = answerDomainInfo.getPhase();
            exId = answerDomainInfo.getExId();

        }

        // find next consequent (using solved facts)
        List<BackendFactEntity> solution = q.getSolutionFacts();
        assertNotNull(solution, "Call solve() question before getAnyNextCorrectAnswer() !");

        solution = new ArrayList<>(solution);
        solution.addAll(q.getStatementFacts());
        OntModel model = factsToOntModel(solution);

        // find any consequent of last correct act ...
        // get shortcuts to properties
        OntProperty boundary_of = model.getOntProperty(model.expandPrefix(":boundary_of"));
        OntProperty begin_of = model.getOntProperty(model.expandPrefix(":begin_of"));
        OntProperty end_of = model.getOntProperty(model.expandPrefix(":end_of"));
        OntProperty id = model.getOntProperty(model.expandPrefix(":id"));

        Individual actionFrom = model.listResourcesWithProperty(id, Integer.parseInt(exId)).nextResource().as(Individual.class);

        String consequentPropName = "always_consequent"; // "consequent";

        String qaInfoPrefix;
        // check if actionFrom is an expr; if so, find its current value and update `consequentPropName` accordingly
        if (actionFrom.hasOntClass(model.getOntClass(model.expandPrefix(":expr")))) {
            qaInfoPrefix = phase + ":" + exId;
            int count = 0;

            for (ResponseEntity response : responsesForTrace(q.getQuestionData(), false)) {
                AnswerObjectEntity answerObj = response.getLeftAnswerObject();
                String domainInfo = answerObj.getDomainInfo();
                if (domainInfo.startsWith(qaInfoPrefix)) {
                    ++ count;
                }
            }

            String exprName = getExpressionNameById(q.getQuestionData(), Integer.parseInt(exId));
            int exprVal = getValueForExpression(q.getQuestionData(), exprName, count);

            // use appropriate property name
            if (exprVal == 1)
                consequentPropName = "on_true_consequent";
            else  // == 0
                consequentPropName = "on_false_consequent";
        }

        OntProperty consequent = model.getOntProperty(model.expandPrefix(":" + consequentPropName));

        Individual boundFrom = model.listResourcesWithProperty(
                phase.equalsIgnoreCase("started") ? begin_of : end_of,
                actionFrom).nextResource().as(Individual.class);

        // true / false ways do matter in case of expr
        Individual boundTo = boundFrom.getPropertyResourceValue(consequent).as(Individual.class);

        Individual actionTo = boundTo.getPropertyResourceValue(boundary_of).as(Individual.class);

        String idTo = actionTo.getPropertyValue(id).asLiteral().getLexicalForm();
        String phaseTo = boundTo.hasProperty(begin_of) ? "started" : "finished";
        // check if actionTo is stmt/expr
        if (phaseTo.equals("started") && (
                actionTo.hasOntClass(model.getOntClass(model.expandPrefix(":stmt"))) || actionTo.hasOntClass(model.getOntClass(model.expandPrefix(":expr")))
        )) {
            phaseTo = "performed";
        }

        // next correct answer found: question answer domain info
        qaInfoPrefix = phaseTo + ":" + idTo;

        // find reason for correct action (a deeper subproperty of consequent assigned in parallel)
        String reasonName = null;
        for (StmtIterator it = model.listStatements(boundFrom, null, boundTo); it.hasNext(); ) {
            Property prop = it.nextStatement().getPredicate();
            if (consequent.hasSubProperty(prop, false)) {
                reasonName = prop.getLocalName();
                break;
            }
        }

        // get placeholder values for this bound-bound transition
        RDFNode reason_node = null;
        List<RDFNode> reason_nodes = model.listStatements(boundFrom, model.getProperty(model.expandPrefix(":to_reason")),
                (RDFNode) null).toList().stream().map(Statement::getObject).collect(Collectors.toList());
        reason_nodes.retainAll(model.listStatements(null, model.getProperty(model.expandPrefix(":from_reason")),
                boundTo).toList().stream().map(Statement::getSubject).collect(Collectors.toList()));

        if (reason_nodes.isEmpty()) {
            System.out.println("reason_node was not found for reason: " + reasonName);
        } else {
            reason_node = reason_nodes.get(0);
        }
        HashMap<String, String> placeholders = new HashMap<>();
        if (reason_node != null) {
            for (StmtIterator it = model.listStatements((Resource) reason_node, null, (String) null); it.hasNext(); ) {
                Statement statement = it.next();
                String verb = statement.getPredicate().getLocalName();
                if (getFieldProperties().contains(verb)) {
                    String fieldName = verb.replaceAll("field_", "");
                    String value = statement.getString();
                    value = "\"" + value + "\"";
                    if (placeholders.containsKey(fieldName)) {
                        // append to previous data
                        value = placeholders.get(fieldName) + ", " + value;
                    }
                    placeholders.put(fieldName, value);
                }
            }
        }


        //// System.out.println("next correct answer found: " + qaInfoPrefix);

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
        correctAnswer.lawName = reasonName; // "No correct law yet, using flow graph";  // answerImpl.lawName;

        HyperText explanation;
        if (getFrontMessages().containsKey(reasonName)) {
            Language userLang;  // natural language to format explanation
            try {
                userLang = q.getQuestionData().getExerciseAttempt().getUser().getPreferred_language(); // The language currently selected in UI
            } catch (NullPointerException e) {
                userLang = Language.ENGLISH;  // fallback if it cannot be figured out
            }
            String message = getFrontMessages().get(reasonName).get(userLang);
            // fill in the blanks
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("<" + entry.getKey() + ">", entry.getValue());
            }
            explanation = new HyperText(message);
        }
        else
            explanation = new HyperText("explanation for " + Optional.ofNullable(reasonName).orElse("<unknown reason>") + ": not found in domain localization");
        correctAnswer.explanation = explanation; // getCorrectExplanation(answerImpl.lawName);
        return correctAnswer;
    }

//    public CorrectAnswer getRemainingCorrectAnswers(Question q) {
//
//        return null;
//    }

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
                if (fact.getVerb().equals("stmt_name")) {
                    stmt_name = fact.getObject();
                }
            }
        }
        if (isEXpr)
            return stmt_name;
        return null;
    }

    /**
     *
     * @param question
     * @param expressionName
     * @param executionTime 1-based number
     * @return
     */
    public int getValueForExpression(QuestionEntity question, String expressionName, int executionTime) {
        for (BackendFactEntity fact : question.getStatementFacts()) {
            if (fact.getSubject().equals(expressionName) && fact.getVerb().equals("not-for-reasoner:expr_values") && fact.getObjectType().equals("List<boolean>")) {
                String values = fact.getObject();
                String[] tokens = values.split(",");
                if (0 < executionTime && executionTime <= tokens.length) {
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

    private Map<String, LocalizationMap> getFrontMessages() {
        if (MESSAGES == null) {
            MESSAGES = new HashMap<>();

            try(BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(MESSAGES_CONFIG_PATH), StandardCharsets.UTF_8))) {

                for(String line; (line = br.readLine()) != null; ) {
                    // format-specific parsing
                    line = line.trim();
                    if (!line.isEmpty()) {
                        String[] parts = line.split("\t", 3);
                        if (parts.length == 3) {

                            String[] names = parts[0].split(" ", 2);
                            String name = names[0];  // get EN name; RU at [1]

                            LocalizationMap lm = new LocalizationMap();
                            lm.put(Language.RUSSIAN, parts[1]); // get RU msg
                            lm.put(Language.ENGLISH, parts[2]); // get EN msg

                            MESSAGES.put(name, lm);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("ERROR reading messages from file: " + MESSAGES_CONFIG_PATH);
                e.printStackTrace();
            }
        }
        return MESSAGES;
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
