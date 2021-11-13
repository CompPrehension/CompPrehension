package org.vstu.compprehension.models.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.text.StringSubstitutor;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.jetbrains.annotations.Nullable;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.utils.ApplicationContextProvider;
import org.vstu.compprehension.utils.HyperText;

import javax.inject.Singleton;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.getLeafOntClasses;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.testSubClassOfTransitive;
import static org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph.factsListDeepCopy;

@Component @Log4j2
@Singleton
public class ControlFlowStatementsDomain extends Domain {
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String EXECUTION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderActsSupplementary";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
//    static final String LAWS_CONFIG_PATH = "file:c:/D/Work/YDev/CompPr/c_owl/jena/domain_laws.json";
    static final String LAWS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-laws.json";
    public static final String MESSAGES_CONFIG_PATH = "classpath:/org/vstu/compprehension/models/businesslogic/domains/control-flow-messages";

    static final String MESSAGE_PREFIX = "ctrlflow_";

    // dictionary
    static final String VOCAB_SCHEMA_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-schema.rdf";
    private static DomainVocabulary VOCAB = null;

    static final String QUESTIONS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-questions.json";
    static List<Question> QUESTIONS;
    private static List<String> reasonPropertiesCache = null;
    private static List<String> fieldPropertiesCache = null;

    private final LocalizationService localizationService;

    public ControlFlowStatementsDomain(@Autowired LocalizationService localizationService) {
        super();
        this.localizationService = localizationService;
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
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
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

//        final String textMode = "text";
        final String textMode = "html";

        Language lang = getUserLanguage(question);

        ArrayList<HyperText> result = new ArrayList<>();

        String qType = question.getQuestionData().getQuestionDomainType();
        if (qType.equals(EXECUTION_ORDER_QUESTION_TYPE) || qType.equals("Type" + EXECUTION_ORDER_QUESTION_TYPE)) {
            HashMap<String, Integer> exprName2ExecTime = new HashMap<>();
            FactsGraph qg = new FactsGraph(question.getQuestionData().getStatementFacts());
            final List<String> actionKinds = List.of("stmt", "expr", "loop", "alternative","if","else-if","else", "iteration" /* << iteration is not-a-class */);

            for (ResponseEntity response : responsesForTrace(question.getQuestionData(), true)) {

                boolean responseIsWrong = ! response.getInteraction().getViolations().isEmpty();

                // format a trace line ...

                AnswerObjectEntity answerObj = response.getLeftAnswerObject();
                String domainInfo = answerObj.getDomainInfo();
                AnswerDomainInfo info = new AnswerDomainInfo(domainInfo).invoke();
                String line;
                String actionKind = answerObj.getConcept(); // temporary setting
                if (actionKind.endsWith("loop"))
                    actionKind = "loop";
                // find most general type
                ArrayList<String> thisActionKinds = qg.chainReachable(info.getExId(), List.of("^id", "rdf:type"));
                thisActionKinds.add(actionKind);
                thisActionKinds.retainAll(actionKinds);
                if (thisActionKinds.isEmpty()) {
                    actionKind = "iteration";  // todo: verify
                } else {
                    actionKind = thisActionKinds.get(0);
                }


                String lineTpl = getMessage(textMode + ".trace." + actionKind + "." + info.getPhase(), lang); // pass locale

                Map<String, String> replacementMap = new HashMap<>();

                int execTime = 1 + exprName2ExecTime.getOrDefault(domainInfo, 0);
                exprName2ExecTime.put(domainInfo, execTime);
                replacementMap.put("n", String.valueOf(execTime));

                String name = getActionNameById(question.getQuestionData(), Integer.parseInt(info.getExId()), /*actionKind*/ answerObj.getConcept());
                replacementMap.put("name", name);

                if (lineTpl.contains("nth_time")) {
                    String nth_time_template = getMessage(textMode + ".trace.template.nth_time", lang); // pass locale
                    String nth_time = formatTemplate(nth_time_template, execTime);
                    replacementMap.put("nth_time", nth_time);
                }

                if (lineTpl.contains("cond.name")) {
                    List<String> nameAsList = qg.chainReachable(info.getExId(), List.of("^id", "cond", "stmt_name"));
                    String cond_name = nameAsList.isEmpty()? "!none!" : nameAsList.get(0);
                    replacementMap.put("cond.name", cond_name);
                }

                if (lineTpl.contains("parent.name")) {
                    String childProp = (actionKind.contains("if") || actionKind.contains("else"))? "branches_item" : actionKind.equals("iteration") ? "body" : null; // use with caution
                    List<String> nameAsList = qg.chainReachable(info.getExId(), List.of("^id", "^" + childProp, "stmt_name"));
                    String parent_name = nameAsList.isEmpty()? "!none!" : nameAsList.get(0);
                    replacementMap.put("parent.name", parent_name);
                }

                // add expression value if necessary
                if (actionKind.equals("expr")) {
                    String valueStr;
                    if (responseIsWrong) {
                        // "not evaluated" / "не вычислено" / ...
                        valueStr = getMessage("value.invalid", lang); //pass locale;
                    } else {
                        int value = getValueForExpression(question.getQuestionData(), name, execTime);

                        // "true" : "false" /  "истина" : "ложь";
                        valueStr = getMessage("value.bool." + value, lang); // pass locale;
                    }
                    replacementMap.put("value", valueStr);
                }

                line = replaceInString(lineTpl, replacementMap);

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

        List<ResponseEntity> responses = new ArrayList<>();

        List<InteractionEntity> interactions = q.getInteractions();

        if (interactions == null || interactions.isEmpty()) {
            return responses; // empty so far
            // early exit: no further checks for emptiness
        }

//        InteractionEntity lastCorrectInteraction = null;

        responses = Optional.of(interactions).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0) // select only interactions without mistakes
                .reduce((first, second) -> second)
                .map(InteractionEntity::getResponses)
                .map(ArrayList::new)  // make a shallow copy so that it can be safely modified
                .orElseGet(ArrayList::new);


        if (allowLastIncorrect) {
            val latestStudentResponse = Optional.of(interactions).stream()
                    .flatMap(Collection::stream)
                    .reduce((first, second) -> second).orElse(null);
            if (latestStudentResponse != null && !latestStudentResponse.getViolations().isEmpty()) {
                // lastInteraction is wrong
                val responseNew = Optional.ofNullable(latestStudentResponse.getResponses())//.stream()
//                        .flatMap(Collection::stream)
                        .filter(resp -> resp.size() > 0)
                        .map(resp -> resp.get(resp.size() - 1))
                        .orElse(null);
                if (responseNew != null) {
                    responses.add(responseNew);
                }
            }
        }
        return responses;
    }

    private String htmlStyled(String styleClass, String text) {
        return "<span class=\"" + styleClass + "\">" + text + "</span>";
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
                val baseQuestionText = getMessage("ORDER_question_prompt", userLanguage);
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
        String msg = getMessage(lawName, userLang);

        if (msg == null) {
            return new HyperText("[Empty explanation] for law " + lawName);
        }

        // Build replacement map
        Map<String, String> replacementMap = new HashMap<>();
        for (ExplanationTemplateInfoEntity template : violation.getExplanationTemplateInfo()) {
            replacementMap.put(template.getFieldName(), template.getValue());
        }

        // Replace in msg
        msg = replaceInString(msg, replacementMap);
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

            // append the latest response to list of correct responses
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
                            exprName = getActionNameById(q, Integer.parseInt(exId), "expr");
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

    Set<String> possibleMistakesByLaw(String correctLaw) {
        return notHappenedMistakes(List.of(correctLaw), null);
    }

    Set<String> notHappenedMistakes(List<String> correctLaws) {
        return notHappenedMistakes(correctLaws, null);
    }

    Set<String> notHappenedMistakes(List<String> correctLaws, List<BackendFactEntity> questionFacts) {
        HashSet<String> mistakeNames = new HashSet<>();

        // may omit context mistakes in the mode when no question info provided
        if (questionFacts != null) {
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
        }
        // use heuristics to get possible mistakes
        for (String corrLaw : correctLaws) {
            switch (corrLaw) {
                // TODO: fix typo in all places: Condtion
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
                    mistakeNames.add("BranchNotNextToCondition");
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
                    mistakeNames.add("BranchOfFalseCondition");
                    mistakeNames.add("LastConditionIsFalseButNoElse");
                    break;
                case("IterationBeginOnTrueCond"):
                    mistakeNames.add("NoIterationAfterSuccessfulCondition");
                    mistakeNames.add("LoopEndAfterSuccessfulCondition");
                    break;
                case("LoopEndOnFalseCond"):
                    mistakeNames.add("NoLoopEndAfterFailedCondition");
                    mistakeNames.add("LoopContinuedAfterFailedCondition");
                    mistakeNames.add("IterationAfterFailedCondition");
                    mistakeNames.add("NoConditionAfterIteration");
                    mistakeNames.add("NoConditionBetweenIterations");
                    break;
                case("PreCondLoopBegin"):
                    mistakeNames.add("LoopEndsWithoutCondition");
                    mistakeNames.add("LoopStartIsNotCondition");
                    break;
                case("PostCondLoopBegin"):
                    mistakeNames.add("LoopEndsWithoutCondition");
                    mistakeNames.add("LoopStartIsNotIteration");
                    break;
                case("LoopCondBeginAfterIteration"):
                    mistakeNames.add("LoopEndsWithoutCondition");
                    mistakeNames.add("NoConditionAfterIteration");
                    mistakeNames.add("NoConditionBetweenIterations");
                    break;
            }
        }
        return mistakeNames;
    }

    @Override
    public boolean needSupplementaryQuestion(ViolationEntity violation) {
        return false;
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
        // there is always one correct answer
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

            // 2) find length of the shortest path over algorithm to the end ...
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
                // In Ordering Question, we need left answer objects only.
                .map(r -> r.getLeftAnswerObject())
                .collect(Collectors.toList());

        return getNextCorrectAnswer(q, lastCorrectInteractionAnswers);
    }

    @Nullable
    private CorrectAnswer getNextCorrectAnswer(Question q, @Nullable List<AnswerObjectEntity> correctTraceAnswersObjects) {

        String phase;
        String exId;
        if (correctTraceAnswersObjects == null || correctTraceAnswersObjects.isEmpty()) {
            // get first act of potential trace
            List<AnswerObjectEntity> answers = q.getQuestionData().getAnswerObjects();
            AnswerObjectEntity firstAnswer = answers.get(0);
            String domainInfo = firstAnswer.getDomainInfo();
            AnswerDomainInfo answerDomainInfo = new AnswerDomainInfo(domainInfo).invoke();

            phase = answerDomainInfo.getPhase();
            exId = answerDomainInfo.getExId();

        } else {
            //// = correctTraceAnswers.get(correctTraceAnswers.size() - 1).answers.get(0).getLeft();
            AnswerObjectEntity lastAnswer = correctTraceAnswersObjects
                    .stream()
                    .reduce((first, second) -> second)
                    .orElse(null);

            assertNotNull(lastAnswer);

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

            // count current exec_time using given steps
            if (correctTraceAnswersObjects != null) {
                for (AnswerObjectEntity answerObj : correctTraceAnswersObjects) {
                    String domainInfo = answerObj.getDomainInfo();
                    if (domainInfo.startsWith(qaInfoPrefix)) {
                        ++count;
                    }
                }
            }
/*          old variant, using student's progress on the question
            for (ResponseEntity response : responsesForTrace(q.getQuestionData(), false)) {
                AnswerObjectEntity answerObj = response.getLeftAnswerObject();
                String domainInfo = answerObj.getDomainInfo();
                if (domainInfo.startsWith(qaInfoPrefix)) {
                    ++ count;
                }
            }
*/

            String exprName = getActionNameById(q.getQuestionData(), Integer.parseInt(exId), "expr");
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

        // check if we encountered the end of the program
        OntProperty any_consequent = model.getOntProperty(model.expandPrefix(":consequent"));
        if (null == boundTo.getPropertyResourceValue(any_consequent)) {
            // the last act of global_code has no outgoing properties, so hide it from outer code by reporting the end now
            return null;
        }

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
        ArrayList<CorrectAnswer.Response> answers = new ArrayList<>();  // lastCorrectInteractionAnswers;
        for (AnswerObjectEntity answer : q.getAnswerObjects()) {
            if (answer.getDomainInfo().startsWith(qaInfoPrefix)) {
                answers.add(new CorrectAnswer.Response(answer, answer));
            }
        }

        // make result
        CorrectAnswer correctAnswer = new CorrectAnswer();
        correctAnswer.question = q.getQuestionData();
        correctAnswer.answers = answers;
        correctAnswer.lawName = reasonName; // "No correct law yet, using flow graph";  // answerImpl.lawName;

        HyperText explanation;
        if (localMessageExists(reasonName)) {
            Language userLang = getUserLanguage(q);
            String message = getMessage(reasonName, userLang);
            // Replace in message
            message = replaceInString(message, placeholders);

            explanation = new HyperText(message);
        }
        else {
            explanation = new HyperText("explanation for " + Optional.ofNullable(reasonName).orElse("<unknown reason>") + ": not found in domain localization");
        }
        correctAnswer.explanation = explanation; // getCorrectExplanation(answerImpl.lawName);
        return correctAnswer;
    }

    private Language getUserLanguage(Question q) {
        Language userLang;  // natural language to format explanation
        try {
            userLang = q.getQuestionData().getExerciseAttempt().getUser().getPreferred_language(); // The language currently selected in UI
        } catch (NullPointerException e) {
            userLang = Language.ENGLISH;  // fallback if it cannot be figured out
        }
        return userLang;
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
            java.util.Collections.sort(possibleViolationsSorted);
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
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
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
    private String getActionNameById(QuestionEntity question, int actionId, String actionRdfType) {
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
                if (fact.getVerb().equals("rdf:type") && fact.getObject().equals(actionRdfType)) {
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

    private String getMessage(String message_text, Language preferred_language) {

        return localizationService.getMessage(MESSAGE_PREFIX + message_text, Language.getLocale(preferred_language));
    }

    /**
     * Check if a message is in a default (ENGLISH) locale
     * @param message_text message key
     * @return true if message exists in ENGLISH locale
     */
    private boolean localMessageExists(String message_text) {
        return !(getMessage(message_text, Language.ENGLISH).equals(MESSAGE_PREFIX + message_text));
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
        return stringSubstitutor.replace(s);
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
        System.out.println(resolvedString);

        // change the map
        valuesMap.put("target", "pink pig");

        // Replace again
        resolvedString = stringSubstitutor.replace(templateString);
        System.out.println(resolvedString);

        MessageFormat form = new MessageFormat("The disk \"{1}\" contains {0,choice,0#no files|1#one file|1<{0,number,integer} files}.");
//        double[] filelimits = {0,1,2};
//        String[] filepart = {"no files","one file","{0,number} files"};
//        ChoiceFormat fileform = new ChoiceFormat(filelimits, filepart);
//        form.setFormatByArgumentIndex(0, fileform);

        int fileCount = 12;
        String diskName = "MyDisk";
        Object[] testArgs = {(long) fileCount, diskName};

        System.out.println(form.format(testArgs));

    }

    public static void main(String[] args) {
        if (true)
            _test_Substitutor();
        else {
            ControlFlowStatementsDomain d = ApplicationContextProvider.getApplicationContext().getBean(ControlFlowStatementsDomain.class);
            d.getQuestionTemplates();
            VOCAB.classDescendants("Erroneous");

            JenaBackend jBack = new JenaBackend();
            jBack.createOntology();
            // jBack.getModel().add(VOCAB.getModel());
            jBack.addFacts(modelToFacts(VOCAB.getModel()));
            jBack.addFacts(QUESTIONS.get(0).getStatementFacts());
//        jBack.debug_dump_model("question");
        }
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
