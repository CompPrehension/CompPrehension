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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opentest4j.AssertionFailedError;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph;
import org.vstu.compprehension.models.businesslogic.storage.LocalRdfStorage;
import org.vstu.compprehension.models.businesslogic.storage.QuestionMetadataManager;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.utils.ApplicationContextProvider;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.retainLeafOntClasses;
import static org.vstu.compprehension.models.businesslogic.domains.DomainVocabulary.testSubClassOfTransitive;
import static org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph.factsListDeepCopy;
@Log4j2
public class ControlFlowStatementsDomain extends Domain {
    public static final String LOCALE_KEY_MARK = "!{locale:";
    static final String RESOURCES_LOCATION = "org/vstu/compprehension/models/businesslogic/domains/";
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String EXECUTION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderActsSupplementary";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
    static final String LAWS_CONFIG_PATH = RESOURCES_LOCATION + "control-flow-statements-domain-laws.json";
    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "control-flow-messages";

    static final String MESSAGE_PREFIX = "ctrlflow_";

    // dictionary
    public static final String VOCAB_SCHEMA_PATH = RESOURCES_LOCATION + "control-flow-statements-domain-schema.rdf";
    private static DomainVocabulary VOCAB = null;
    public static DomainVocabulary getVocabulary() {
        return VOCAB;
    }

    public static final String QUESTIONS_CONFIG_PATH = RESOURCES_LOCATION + "control-flow-statements-domain-questions.json";
    static List<Question> QUESTIONS;

    private static List<String> reasonPropertiesCache = null;
    private static List<String> fieldPropertiesCache = null;

    private final LocalizationService localizationService;

    public ControlFlowStatementsDomain(
            DomainEntity domainEntity,
            LocalizationService localizationService,
            RandomProvider randomProvider,
            QuestionMetadataRepository questionMetadataRepository) {
        super(domainEntity, randomProvider);

        this.localizationService = localizationService;
        this.rdfStorage = new LocalRdfStorage(
                domainEntity, questionMetadataRepository, new QuestionMetadataManager(this, questionMetadataRepository));

        fillConcepts();
        readLaws(this.getClass().getClassLoader().getResourceAsStream(LAWS_CONFIG_PATH));
        // using update() as init
        // OFF: // update();
    }

    public static void initVocab() {
        if (VOCAB == null) {
            VOCAB = new DomainVocabulary(VOCAB_SCHEMA_PATH);
        }
    }

    private void fillTags() {
        tags = new HashMap<>();
        // assign mask bits to Tags
        for (val nameBit : _getTagsName2bit().entrySet()) {
            Tag tag = new Tag();
            tag.setName(nameBit.getKey());
            tag.setBitmask(nameBit.getValue());
            tags.put(tag.getName(), tag);
        }
    }

    private void fillConcepts() {
        concepts = new HashMap<>();
        initVocab();
        addConcepts(VOCAB.readConcepts());

        // add concepts about expressions present in algorithms
        int flags = Concept.FLAG_VISIBLE_TO_TEACHER;
        int flagsAll = Concept.FLAG_VISIBLE_TO_TEACHER | Concept.FLAG_TARGET_ENABLED;
        Concept nested_loop = new Concept("nested_loop", List.of(), flagsAll);
        Concept exprC = new Concept("exprs_in_use", List.of(), flags);
        List<Concept> bases = List.of(exprC);
        addConcepts(List.of(
                nested_loop,
                exprC,
                new Concept("expr:pointer", bases, flags),
                new Concept("expr:func_call", bases, flags),
                new Concept("expr:explicit_cast", bases, flags),
                new Concept("expr:array", bases, flags),
                new Concept("expr:class_member_access", bases, flags)
        ));

        fillConceptTree();

        // assign mask bits to Concepts
        val name2bit = _getConceptsName2bit();
        for (Concept t : concepts.values()) {
            val name = t.getName();
            if (name2bit.containsKey(name)) {
                t.setBitmask(name2bit.get(name));
            }
        }
    }

    private void readLaws(InputStream inputStream) {
        Objects.requireNonNull(inputStream);
        positiveLaws = new HashMap<>();
        negativeLaws = new HashMap<>();

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
                positiveLaws.put(lawForm.getName(), (PositiveLaw) lawForm);
            } else {
                negativeLaws.put(lawForm.getName(), (NegativeLaw) lawForm);
            }
        }

        // add empty laws that name each possible error
        for (String errClass : VOCAB.classDescendants("Erroneous")) {
            negativeLaws.put(errClass, new NegativeLaw(errClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null));
        }


        fillLawsTree();

        // assign mask bits to Laws
//        for (Law t : positiveLaws.values()) {
//            val name = t.getName();
//            if (name2bit.containsKey(name)) {
//                t.setBitmask(name2bit.get(name));
//            }
//        }
        val name2bit = _getViolationsName2bit();
        for (Law t : negativeLaws.values()) {
            val name = t.getName();
            if (name2bit.containsKey(name)) {
                t.setBitmask(name2bit.get(name));
            }
        }
    }

    @Override
    public List<Concept> getLawConcepts(Law law) {
        return null;
    }

    @Override
    public void update() {
        // init questions storage
        getRdfStorage();
    }

    @Override
    public Model getSchemaForSolving() {
        return VOCAB.getModel();
    }

    @Override
    public String getDefaultQuestionType(boolean supplementary) {
        return supplementary
                ? EXECUTION_ORDER_SUPPLEMENTARY_QUESTION_TYPE
                : EXECUTION_ORDER_QUESTION_TYPE;
    }

    @Override
    public List<CorrectAnswer> getAllAnswersOfSolvedQuestion(Question question) {

        ArrayList<CorrectAnswer> result = new ArrayList<>();

        String qType = question.getQuestionData().getQuestionDomainType();
        if (qType.equals(EXECUTION_ORDER_QUESTION_TYPE) || qType.equals("Type" + EXECUTION_ORDER_QUESTION_TYPE)) {
            // gather correct steps
            List<AnswerObjectEntity> correctTraceAnswersObjects = new ArrayList<>();
            OntModel model = modelToOntModel(getSolutionModelOfQuestion(question));

            while (true) {
                System.out.println("Getting getNextCorrectAnswer having " + correctTraceAnswersObjects.size());
                CorrectAnswer ca = getNextCorrectAnswer(question, correctTraceAnswersObjects, model);
                if (ca == null)
                    break;
                result.add(ca);

                AnswerObjectEntity answerObj = ca.answers.get(0).getLeft(); // one answer, anyway
                correctTraceAnswersObjects.add(answerObj);
            }
        }

        return result;
    }

    @Override
    public List<HyperText> getCompleteSolvedTrace(Question question) {
//        final String textMode = "text";
        final String textMode = "html";

        Language lang = getUserLanguage(question);

        ArrayList<HyperText> result = new ArrayList<>();

        String qType = question.getQuestionData().getQuestionDomainType();
        if (qType.equals(EXECUTION_ORDER_QUESTION_TYPE) || qType.equals("Type" + EXECUTION_ORDER_QUESTION_TYPE)) {
            HashMap<String, Integer> exprName2ExecTime = new HashMap<>();
            FactsGraph qg = new FactsGraph(question.getQuestionData().getStatementFacts());
            // TODO: add here new types of actions (hardcoded) ...
            final List<String> actionKinds = List.of("stmt", "expr", "loop", "alternative","if","else-if","else", "return", "break", "continue", "iteration" /* << iteration is not-a-class */);

            // gather correct steps
            List<AnswerObjectEntity> correctTraceAnswersObjects = new ArrayList<>();
            Model model = getSolutionModelOfQuestion(question);

            while (true) {
                System.out.println("Getting getNextCorrectAnswer № " + correctTraceAnswersObjects.size());
                CorrectAnswer ca = getNextCorrectAnswer(question, correctTraceAnswersObjects, modelToOntModel(model));
                if (ca == null)
                    break;

                AnswerObjectEntity answerObj = ca.answers.get(0).getLeft(); // one answer, anyway
                correctTraceAnswersObjects.add(answerObj);

                // format a trace line ...

                HyperText htext = _formatTraceLine(question.getQuestionData(), textMode, lang, exprName2ExecTime, qg,
                        actionKinds, answerObj, false);
                result.add(htext);
            }
        }

        return result;
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
            // TODO: add here new types of actions (hardcoded) ...
            final List<String> actionKinds = List.of("stmt", "expr", "loop", "alternative","if","else-if","else", "return", "break", "continue", "iteration" /* << iteration is not-a-class */);

            for (ResponseEntity response : responsesForTrace(question.getQuestionData(), true)) {

                AnswerObjectEntity answerObj = response.getLeftAnswerObject();
                boolean responseIsWrong = ! response.getInteraction().getViolations().isEmpty();

                // format a trace line ...

                HyperText htext = _formatTraceLine(question.getQuestionData(), textMode, lang, exprName2ExecTime, qg,
                        actionKinds, answerObj, responseIsWrong);
                result.add(htext);
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

    private HyperText _formatTraceLine(QuestionEntity question, String textMode, Language lang,
                                       HashMap<String, Integer> exprName2ExecTime, FactsGraph qg, List<String> actionKinds,
                                       AnswerObjectEntity answerObj, boolean lineIsWrong) {
//        AnswerObjectEntity answerObj = response.getLeftAnswerObject();
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

        String name = getActionNameById(question, Integer.parseInt(info.getExId()), /*actionKind*/ answerObj.getConcept());
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

        if (lineTpl.contains("return_expr")) {
            assert name != null;
            String return_expr = name.replace("return", "").trim();
            replacementMap.put("return_expr", return_expr);
        }

        // add expression value if necessary
        if (actionKind.equals("expr")) {
            String valueStr;
            if (lineIsWrong) {
                // "not evaluated" / "не вычислено" / ...
                valueStr = getMessage("value.invalid", lang); //pass locale;
            } else {
                int value = getValueForExpression(question, name, execTime);

                // "true" : "false" /  "истина" : "ложь";
                valueStr = getMessage("value.bool." + value, lang); // pass locale;
            }
            replacementMap.put("value", valueStr);
        }

        line = replaceInString(lineTpl, replacementMap);

        line = line.replaceAll("  ", " ");  // collapse multiple spaces if any

        // check if this line is wrong
        if (lineIsWrong) {
            // add background color
            line = htmlStyled("warning", line);
        }

        return new HyperText(line);
    }

    private List<ResponseEntity> responsesForTrace(QuestionEntity q, boolean allowLastIncorrect) {

        List<ResponseEntity> responses = new ArrayList<>();

        var interactions = q.getInteractions();

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
                questionRequest = fillBitmasksInQuestionRequest(questionRequest);
                foundQuestions = getRdfStorage().searchQuestions(this, questionRequest, randomPoolSize);

                // search again if nothing found with "TO_COMPLEX"
                SearchDirections lawsSearchDir = questionRequest.getLawsSearchDirection();
                if (foundQuestions.isEmpty() && lawsSearchDir == SearchDirections.TO_COMPLEX) {
                    questionRequest.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
                    foundQuestions = getRdfStorage().searchQuestions(this, questionRequest, randomPoolSize);
                }
                log.info("Autogenerated questions found: " + foundQuestions.size());
            } catch (RuntimeException ex) {
                // file storage was not configured properly...
                ex.printStackTrace();
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
        Question questionCopy = makeQuestionCopy(res, questionRequest.getExerciseAttempt(), userLanguage);

        //// patch question text for survey: hide comments
        // questionCopy.getQuestionData().setQuestionText(
        //         questionCopy.getQuestionText().getText().replace(
        //                 "span.comment {",
        //                 "span.comment { display: none;"
        //         )
        // );

        log.info("CtrlFlow domain has prepared the question: " + questionCopy.getQuestionName());

        return questionCopy;
    }

    @Override
    public QuestionRequest fillBitmasksInQuestionRequest(QuestionRequest qr) {
        qr = super.fillBitmasksInQuestionRequest(qr);

        // set trace concepts to search, not [formulation] concepts
        qr.setTraceConceptsTargetedBitmask(qr.getConceptsTargetedBitmask());
        qr.setConceptsTargetedBitmask(0L);

        // hard limits on solution length (questions outside this boundaries will never appear)
        qr.setStepsMin(3);
        qr.setStepsMax(30);

        return qr;
    }

    /*static List<BackendFactEntity> schemaFactsCache = null;

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
    }*/

    protected Question makeQuestionCopy(Question q, ExerciseAttemptEntity exerciseAttemptEntity, Language userLanguage) {
        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showSupplementaryQuestions(false)
                .showTrace(true)
                .multipleSelectionEnabled(true)
                //.requireAllAnswers(false)
                .orderNumberOptions(new OrderQuestionOptionsEntity.OrderNumberOptions("/", OrderQuestionOptionsEntity.OrderNumberPosition.NONE, null))
                .templateId(q.getQuestionData().getOptions().getTemplateId())  // copy from loaded question
                .questionMetaId(q.getQuestionData().getOptions().getQuestionMetaId())
                .metadata(q.getQuestionData().getOptions().getMetadata())  // copy from loaded question
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
            String di = answerObjectEntity.getDomainInfo();
            if (di.length() >= 1000)
                di = di.substring(0, 998);  // hack
            newAnswerObjectEntity.setDomainInfo(di);
            String ht = answerObjectEntity.getHyperText();
            if (ht.length() >= 255)
                ht = di.substring(0, 253);  // hack
            newAnswerObjectEntity.setHyperText(ht);
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

        // DON'T: add schema facts
        List<BackendFactEntity> facts = new ArrayList<>(/*getSchemaFacts(true)*/);
        // statement facts are already prepared in the Question's JSON
        facts.addAll(factsListDeepCopy(q.getStatementFacts()));
        facts = _patchStatementFacts(facts, userLanguage);
        entity.setStatementFacts(facts);
        entity.setQuestionType(q.getQuestionType());

        switch (q.getQuestionType()) {
            case ORDER:
                var baseQuestionText = getMessage("ORDER_question_prompt", userLanguage);
                if (true) {
                    // DEBUG: add question name as html comment
                    var name = q.getQuestionName();
                    name = "<!-- question name: " + name + " -->";
                    baseQuestionText = name + baseQuestionText;
                }
                entity.setQuestionText(baseQuestionText + q.getQuestionText().getText());
                patchQuestionTextShowValuesInline(entity, userLanguage);  // inject expr values into html
                entity.setOptions(orderQuestionOptions);
                return new Ordering(entity, this);
            case MATCHING:
                entity.setQuestionText((q.getQuestionText().getText()));
                entity.setOptions(matchingQuestionOptions);
                return new Matching(entity, this);
            case MULTI_CHOICE:
                entity.setQuestionText((q.getQuestionText().getText()));
                entity.setOptions(multiChoiceQuestionOptions);
                return new MultiChoice(entity, this);
            default:
                throw new UnsupportedOperationException("Unknown type in ControlFlowStatementsDomain::makeQuestion: " + q.getQuestionType());
        }
    }

    /** show expr values aside the expressions (on the same line) */
    public QuestionEntity patchQuestionTextShowValuesInline(QuestionEntity question, Language lang) {
        String text = question.getQuestionText();
        if (text.contains("<!-- patched: inline expr values -->"))
            return question;

        HashMap<String, String> id2subj = new HashMap<>();
        HashMap<String, String> subj2name = new HashMap<>();
        for (val answerObj : question.getAnswerObjects()) {
            if (answerObj.getConcept().equals("expr")) {
                String domainInfo = answerObj.getDomainInfo();
                AnswerDomainInfo info = new AnswerDomainInfo(domainInfo).invoke();
                val id = info.getExId();
                if (!id2subj.containsKey(id)) {
                    for (BackendFactEntity fact : question.getStatementFacts()) {
                        if (fact.getVerb().equals("id")) {
                            id2subj.put(fact.getObject(), fact.getSubject());
                            if (fact.getObject().equals(id) && subj2name.containsKey(fact.getSubject())) {
                                break;
                            }
                        }
                        if (fact.getVerb().equals("stmt_name")) {
                            subj2name.put(fact.getSubject(), fact.getObject());
                        }
                    }
                }
                String subj = id2subj.get(id);
                String exprName = subj2name.get(subj);
                if (subj == null || exprName == null) {
                    log.info("Cannot find subject in statement facts for id=" + id);
                    continue;
                }

                // find values for this expr
                List<Integer> boolValues = null;
                for (BackendFactEntity fact : question.getStatementFacts()) {
                    if (fact.getSubject().equals(exprName) && fact.getVerb().equals("not-for-reasoner:expr_values") && fact.getObjectType().equals("List<boolean>")) {
                        String values = fact.getObject();
                        boolValues = Arrays.stream(values.split(","))
                                .map(Integer::parseInt).collect(Collectors.toList());
                        break;
                    }
                }
                if (boolValues == null) {
                    // the expr seems to be unreachable (thus there are no values for it)
                    // log.info("Cannot find expr values in statement facts for id=" + id);
                    continue;
                }

                // format "injection"
                String valuesStr = boolValues.stream()
                        .map(value -> getMessage("value.bool." + value, lang))
                        .map(value -> "<span class=\"atom\">" + value + "</span>")
                        .collect(Collectors.joining(", "));
                valuesStr = " &rarr; " + valuesStr + "&nbsp;";
                valuesStr = "<span class=\"compph-debug-info\">" + valuesStr + "</span>";

                // find place where to insert
                /* algorithm_element_id=\"4\" id=\"answer_9\" act_type=\"performed\" ...></span>n++</span>)&nbsp;&nbsp;<span class=\"comment\">// L_7</span></div> */
                Pattern p = Pattern.compile("(algorithm_element_id=\""+id+"\".+?)(?=(?:&nbsp;)*<span class=\"comment\"|</div>)");
                Matcher m = p.matcher(text);
                if (m.find()) {
                    String insertAfter = m.group();
                    text = text.replace(insertAfter, insertAfter + valuesStr);
                }
            }
        }

        // add mark that the questions is already processed
        text += "<!-- patched: inline expr values -->";
        question.setQuestionText(text);

        return question;
    }

    /** repair stmt_name fields - for global code & return/break/ statements */
    private List<BackendFactEntity> _patchStatementFacts(List<BackendFactEntity> facts, Language userLanguage) {

        FactsGraph fg = new FactsGraph(facts);

        // fix names missing stmt kind prefixes
        for (String kind : List.of("return", "break", "continue")) {
            for (BackendFactEntity sf : fg.filterFacts(null, "rdf:type", kind)) {
                for (BackendFactEntity lf : fg.filterFacts(sf.getSubject(), "stmt_name", null)) {
                    String literal = lf.getObject();
                    if (!literal.startsWith(kind)) {
                        String s = kind;
                        if (!literal.isEmpty()) {
                            s += " " + literal;
                        }
                        // update in-place
                        lf.setObject(s);
                    }
                }
            }
        }

        String entryPoint = fg.findOne(null, "entry_point", null).getObject();
        BackendFactEntity epf = fg.findOne(entryPoint, "stmt_name", null);
        //// String s = "global code";
        String s = getMessage("text.global_scope", userLanguage);
        // update in-place
        epf.setObject(s);

        return fg.getFactsAsIs();
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

    /** handle special locale-dependent placeholders */
    @NotNull
    private String replaceLocaleMarks(Language userLang, String value) {
        final int lenLkm = LOCALE_KEY_MARK.length();
        int i, pos = 0;
        while ((i = value.indexOf(LOCALE_KEY_MARK, pos)) > -1) {
            int closingBracePos = value.indexOf("}", i + lenLkm);
            String key = value.substring(i + lenLkm, closingBracePos);
            String replaceTo = getMessage(key, userLang);
            value = value.replace(value.substring(i, closingBracePos + 1), replaceTo);
            pos = i - lenLkm - key.length() - 1 + replaceTo.length();
        }
        return value;
    }

    /** handle possible word duplications (from template and action description inserted) */
    @NotNull
    private String postProcessFormattedMessage(String msg) {
        Pattern p = Pattern.compile("\\s([\\p{L}]+\\s+)\\s*[\"«]?\\s*\\1");  //  [\p{L}] is partial (alphabetical only) unicode replacement of \w (https://stackoverflow.com/a/4305084/12824563)
        Matcher m = p.matcher(msg);
        // remove first of duplicated words
        msg = m.replaceAll(mr -> mr.group(0).replaceFirst(mr.group(1), ""));

        // Capitalize message (its first char)
        for (int i = 0; i < 2; ++i) {
            char ch = msg.charAt(i);
            if (ch == '"' || ch == '«')
                continue;
            val s = msg.substring(i, i+1);
            if (!s.toUpperCase().equals(s)) {
                msg = msg.replaceFirst(s, s.toUpperCase());
            }
            break;
        }
        return msg;
    }

    /**
     * Get all needed (positive and negative) laws for this questionType using default tags
     * @param questionDomainType type of question
     * @return list of laws
     */
    public List<Law> getQuestionLaws(String questionDomainType /*, List<Tag> tags*/) {

        List<Law> laws = new ArrayList<>();
        laws.addAll(positiveLaws.values());
        laws.addAll(negativeLaws.values());
        return laws;
    }

    // filter positive laws by question type and tags
    @Override
    public Collection<PositiveLaw> getQuestionPositiveLaws(String domainQuestionType, List<Tag> tags) {
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

    public Collection<NegativeLaw> getQuestionNegativeLaws(String domainQuestionType, List<Tag> tags) {
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
    public Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        // proxy to static method
        return null /*getSolutionVerbsStatic(questionDomainType, statementFacts)*/;
    }

    public static Set<String> getSolutionVerbsStatic(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            Set<String> verbs = new HashSet<>(Arrays.asList(
//            return new ArrayList<>(Arrays.asList(
                    "rdf:type",
                    "rdfs:subClassOf",
                    "rdfs:subPropertyOf",
                    "rdfs:label",
                    "id",
                    "atom_action",
                    "boundary_of",
                    "begin_of",
                    "end_of",
                    "halt_of",
                    "interrupt_origin",
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
                    "from_reason",
                    "fetch_kind_of_loop"
            ));
            if (reasonPropertiesCache == null)
                reasonPropertiesCache = VOCAB.propertyDescendants("consequent");
            verbs.addAll(reasonPropertiesCache);
            verbs.addAll(getFieldProperties());
            return verbs;
        }
        return new HashSet<>();
    }

    private static List<String> getFieldProperties() {
        if (fieldPropertiesCache == null)
            fieldPropertiesCache = VOCAB.propertyDescendants("string_placeholder");
        return fieldPropertiesCache;
    }

    @Override
    public Set<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return null /*getViolationVerbsStatic(questionDomainType, statementFacts)*/;
    }

    public static Set<String> getViolationVerbsStatic(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            Set<String> verbs = new HashSet<>(Arrays.asList(
                    // - "*whole_model*" //,
                    "rdf:type",
                    "rdfs:subClassOf",
                    "rdfs:subPropertyOf",
                    "rdfs:label",
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
            return verbs;
        }
        return new HashSet<>();
    }

    @Override
    public Collection<Fact> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
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
            Collection<Fact> result = new ArrayList<>();

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
            assert entryPointIri != null;
            // trace object
            String trace = "comp-ph-trace";
            appendActFacts(result, 0, trace, "trace",
                    //// null,
                    entryPointIri.split("_", 2)[0],
                    0, null, trace, null, false);
            result.add(new Fact(
                    "owl:NamedIndividual", trace,
                    "index",
                    "xsd:int", "0"
            ));
            result.add(new Fact(
                    "owl:NamedIndividual", trace,
                    "rdf:type",
                    "owl:Class", "act_begin" // RDFS rules are not included with rules having salience of 10, so cast "trace" to "act_begin" now
            ));
//            make_triple(trace_obj, onto.exec_time, 0)  # set to 0 so next is 1
            result.add(new Fact(
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

    @Override
    public InterpretSentenceResult interpretSentence(Collection<Fact> violations) {
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

    Set<String> notHappenedMistakes(List<String> correctLaws, Collection<Fact> questionFacts) {
        HashSet<String> mistakeNames = new HashSet<>();

        // may omit context mistakes in the mode when no question info provided
        if (questionFacts != null) {
            // find out if context mistakes are applicable here
            for (Fact f : questionFacts) {
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

    private static OntModel modelToOntModel(Model model) {
        OntModel ontModel = ModelFactory.createOntologyModel(OWL_MEM);        ontModel.add(model);
        return ontModel;
    }

    private static OntModel factsAndSchemaToOntModel(Collection<Fact> facts) {
        Model schema = VOCAB.getModel();
        JenaFactList fl = JenaFactList.fromFacts(facts);
        return modelToOntModel(schema.union(fl.getModel()));
    }

    private static Collection<Fact> modelToFacts(Model factsModel) {
        return new JenaFactList(factsModel);
    }

    @Override
    public ProcessSolutionResult processSolution(Collection<Fact> solution) {
        OntModel model = factsAndSchemaToOntModel(solution);

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

            assert !(tripleLastInTraceAsList.isEmpty());
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

                assert !(entryAsList.isEmpty());

                actionInd = entryAsList.get(0).as(Individual.class);
            }

            /// assert actionInd != null;  // did not get last act correctly ?
            assert actionInd != null;

            // 2) find length of the shortest path over algorithm to the end ...
            // get shortcuts to properties
            OntProperty boundary_of = model.getOntProperty(model.expandPrefix(":boundary_of"));
            OntProperty begin_of = model.getOntProperty(model.expandPrefix(":begin_of"));
            OntProperty end_of = model.getOntProperty(model.expandPrefix(":end_of"));
            OntProperty halt_of = model.getOntProperty(model.expandPrefix(":halt_of"));
            OntProperty on_false_consequent = model.getOntProperty(model.expandPrefix(":on_false_consequent"));
            OntProperty consequent = model.getOntProperty(model.expandPrefix(":consequent"));
            // get boundary of the initial action
            Individual bound;
            if (boundRes == null) {
                List<Resource> bounds = model.listSubjectsWithProperty(begin_of, actionInd).toList(); // actionInd
                // .getPropertyResourceValue(boundary_of);
                assert !bounds.isEmpty();
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
                    if (bound.hasProperty(begin_of) && (nextBound.hasProperty(end_of) || nextBound.hasProperty(halt_of))) {
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
    private CorrectAnswer getNextCorrectAnswer(Question q, @Nullable List<AnswerObjectEntity> correctTraceAnswersObjects) {
        return getNextCorrectAnswer(q, correctTraceAnswersObjects, modelToOntModel(getSolutionModelOfQuestion(q)));
    }

    @Nullable
    private CorrectAnswer getNextCorrectAnswer(Question q, @Nullable List<AnswerObjectEntity> correctTraceAnswersObjects, OntModel model) {


        // get shortcuts to properties
        Property boundary_of = model.getProperty(model.expandPrefix(":boundary_of"));
        Property begin_of = model.getProperty(model.expandPrefix(":begin_of"));
        Property end_of = model.getProperty(model.expandPrefix(":end_of"));
        Property id = model.getProperty(model.expandPrefix(":id"));
        Property entry_point = model.getProperty(model.expandPrefix(":entry_point"));

        String phase;
        String exId;
        if (correctTraceAnswersObjects == null || correctTraceAnswersObjects.isEmpty()) {
            // get first act of potential trace

            // Note: first in answerObject list may differ from the first action of the algorithm!

            // rely on entry_point relation here.
            Individual entryStmt = model.listObjectsOfProperty(entry_point).nextNode().as(Individual.class);

            phase = "started";
            exId = model.listObjectsOfProperty(entryStmt, id).nextNode().asLiteral().getLexicalForm();

        } else {
            //// = correctTraceAnswers.get(correctTraceAnswers.size() - 1).answers.get(0).getLeft();
            AnswerObjectEntity lastAnswer = correctTraceAnswersObjects
                    .stream()
                    .reduce((first, second) -> second)
                    .orElse(null);
            String domainInfo = lastAnswer.getDomainInfo();
            AnswerDomainInfo answerDomainInfo = new AnswerDomainInfo(domainInfo).invoke();
            phase = answerDomainInfo.getPhase();
            exId = answerDomainInfo.getExId();
        }

        // find any consequent of last correct act ...
        Individual actionFrom = model.listResourcesWithProperty(id, Integer.parseInt(exId)).nextResource().as(Individual.class);

        String consequentPropName = "always_consequent"; // "consequent";

        String qaInfoPrefix;
        // check if actionFrom is an expr; if so, find its current value and update `consequentPropName` accordingly
        if (actionFrom.hasOntClass(model.createOntResource(model.expandPrefix(":expr")))) {
            qaInfoPrefix = phase + ":" + exId + ":";  // 3rd partition (hypertext) should always present.
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
        Resource bndTO = boundFrom.getPropertyResourceValue(consequent);
        if (bndTO == null) {
            System.out.println("Error walking flow graph...");
            return null;
        }
        Individual boundTo = bndTO.as(Individual.class);

        // check if we encountered the end of the program
        Property any_consequent = model.getProperty(model.expandPrefix(":consequent"));
        if (null == boundTo.getPropertyResourceValue(any_consequent)) {
            // the last act of global_code has no outgoing properties, so hide it from outer code by reporting the end now
            return null;
        }

        Individual actionTo = boundTo.getPropertyResourceValue(boundary_of).as(Individual.class);

        String idTo = actionTo.getPropertyValue(id).asLiteral().getLexicalForm();
        String phaseTo = boundTo.hasProperty(begin_of) ? "started" : "finished";
        // check if actionTo is stmt/expr
        if (/*phaseTo.equals("started") &&*/ (
                actionTo.hasOntClass(model.getOntClass(model.expandPrefix(":stmt"))) || actionTo.hasOntClass(model.getOntClass(model.expandPrefix(":expr"))) || actionTo.hasOntClass(model.getOntClass(model.expandPrefix(":interrupt_action")))
        )) {
            phaseTo = "performed";
        }

        // next correct answer found: question answer domain info
        qaInfoPrefix = phaseTo + ":" + idTo + ":";

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
        Language userLang = getUserLanguage(q);
        HashMap<String, String> placeholders = new HashMap<>();
        if (reason_node != null) {
            for (StmtIterator it = model.listStatements((Resource) reason_node, null, (String) null); it.hasNext(); ) {
                Statement statement = it.next();
                String verb = statement.getPredicate().getLocalName();
                if (getFieldProperties().contains(verb)) {
                    String fieldName = verb.replaceAll("field_", "");
                    String value = statement.getString();
                    value = replaceLocaleMarks(userLang, value);
                    value = "«" + value + "»";
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
                 break; // (?)
            }
        }

        // make result
        CorrectAnswer correctAnswer = new CorrectAnswer();
        correctAnswer.question = q.getQuestionData();
        correctAnswer.answers = answers;
        correctAnswer.lawName = reasonName; // "No correct law yet, using flow graph";  // answerImpl.lawName;

        HyperText explanation;
        if (localMessageExists(reasonName)) {
            String message = getMessage(reasonName, userLang);
            // Replace in message
            message = replaceInString(message, placeholders);
            message = postProcessFormattedMessage(message);
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

    public static List<Question> readQuestions(InputStream inputStream) {
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
    public Question parseQuestionTemplate(InputStream inputStream) {
        RuntimeTypeAdapterFactory<Question> runtimeTypeAdapterFactory =
                RuntimeTypeAdapterFactory
                        .of(Question.class, "questionType")
                        .registerSubtype(Ordering.class, "ORDERING")
                        .registerSubtype(SingleChoice.class, "SINGLE_CHOICE")
                        .registerSubtype(MultiChoice.class, "MULTI_CHOICE")
                        .registerSubtype(Matching.class, "MATCHING");
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

        Question question = gson.fromJson(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                Question.class);

        return question;
    }

    @Override
    public List<Question> getQuestionTemplates() {
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
        boolean isExpr = false;
        for (BackendFactEntity fact : question.getStatementFacts()) {
            if (fact.getSubject().equals(instance)) {
                if (fact.getVerb().equals("rdf:type") && fact.getObject().equals(actionRdfType)) {
                    isExpr = true;
                }
                if (fact.getVerb().equals("stmt_name")) {
                    stmt_name = fact.getObject();
                }
            }
        }
        if (isExpr)
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

    @Override
    public String getMessage(String message_text, Language preferred_language) {

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
        try {
            return stringSubstitutor.replace(s);
        }
        catch (IllegalArgumentException exception) {
            return exception.getMessage() + " - template: " + s + " - placeholders: " + (placeholders.entrySet().stream()).map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
        }
    }

    private HashMap<String, Long> _getTagsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(4);
        name2bit.put("C++", 2L);  	// (2 ^ 1)
        name2bit.put("trace", 4L);  	// (2 ^ 2)
        name2bit.put("ordering", 8L);  	// (2 ^ 3)
        name2bit.put("supplementary", 16L);  	// (2 ^ 4)
        return name2bit;
    }
    private HashMap<String, Long> _getConceptsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(26);
        name2bit.put("pointer", 0x1L);
        name2bit.put("C++", 0x2L);
        name2bit.put("loops", 0x4L);
        name2bit.put("if/else", 0x8L);
        name2bit.put("expr:array", 0x10L);
        name2bit.put("expr:pointer", 0x20L);
        name2bit.put("expr:func_call", 0x40L);
        name2bit.put("expr:explicit_cast", 0x80L);
        name2bit.put("expr:class_member_access", 0x100L);
        name2bit.put("alternative", 0x200L);
        name2bit.put("else", 0x400L);
        name2bit.put("expr", 0x800L);
        name2bit.put("if", 0x1000L);
        name2bit.put("sequence", 0x2000L);
        name2bit.put("return", 0x4000L);
        name2bit.put("loop", 0x8000L);
        name2bit.put("while_loop", 0x10000L);
        name2bit.put("for_loop", 0x20000L);
        name2bit.put("else-if", 0x40000L);
        name2bit.put("nested_loop", 0x80000L);
        name2bit.put("do_while_loop", 0x100000L);
        name2bit.put("break", 0x200000L);
        name2bit.put("continue", 0x400000L);
                 //   stmt       0x800000L
        name2bit.put("seq_longer_than1", 0x1000000L);
        return name2bit;
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
            ControlFlowStatementsDomain d = ApplicationContextProvider.getApplicationContext().getBean(ControlFlowStatementsDomain.class);
            d.getQuestionTemplates();
            VOCAB.classDescendants("Erroneous");

            JenaFactList fl = new JenaFactList(VOCAB.getModel());
            fl.addBackendFacts(QUESTIONS.get(0).getStatementFacts());
        }
    }

}
