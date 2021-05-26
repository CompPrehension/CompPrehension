package org.vstu.compprehension.models.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.utils.HyperText;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Component
public class ControlFlowStatementsDomain extends Domain {
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String EXECUTION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderActsSupplementary";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
//    static final String LAWS_CONFIG_PATH = "file:c:/D/Work/YDev/CompPr/c_owl/jena/domain_laws.json";
    static final String LAWS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-laws.json";

    /// TODO: copy the dictionary to CompPrehension repository
    static final String VOCAB_SCHEMA_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-schema.ttl";
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
        return new ArrayList<>();
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


    Question makeQuestionCopy(Question q, ExerciseAttemptEntity exerciseAttemptEntity) {
        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showTrace(false)
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
        entity.setStatementFacts(q.getStatementFacts());
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
        if (violations.isEmpty())
            return new ArrayList<>();
        else
            return new ArrayList<>(Arrays.asList(new HyperText("A dummy explanation")));
    }

    // filter positive laws by question type and tags
    @Override
    public List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE) || questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
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
        return new ArrayList<>(Collections.emptyList());
    }

    public List<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
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
        return new ArrayList<>(Collections.emptyList());
    }

    @Override
    public List<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "boundary_of",
                    "begin_of",
                    "end_of",
                    "consequent",
                    "has_upcoming",
                    "normal_consequent",
                    "always_consequent",
                    "on_true_consequent",
                    "on_false_consequent",
                    "stmt_name",
                    "corresponding_end",
                    "parent_of",
                    "branches_item",
                    "body",
                    "index"
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
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "name",
                    "id",
                    "rdf:type",
                    "student_corresponding_end",
                    "student_parent_of",
                    "precursor",
                    "cause",
                    "has_causing_condition",
                    "should_be",
                    "should_be_before",
                    "should_be_after",
                    "context_should_be"
            ));
//        } else if (questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
//            return new ArrayList<>(Arrays.asList(
//                    "wrong_type"
//            ));
        }
        return new ArrayList<>();
    }

    @Override
    public List<BackendFactEntity> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            List<BackendFactEntity> result = new ArrayList<>();

            // trace object
            String trace = "comp-ph-trace";
            result.add(new BackendFactEntity(
                    "owl:NamedIndividual",
                    trace,
                    "rdf:type",
                    "owl:Class",
                    "trace"
            ));
            result.add(new BackendFactEntity(
                    "owl:NamedIndividual", trace,
                    "index",
                    "xsd:int", "0"
            ));
            result.add(new BackendFactEntity(
                    "owl:NamedIndividual", trace,
                    "student_index",
                    "xsd:int", "0"
            ));
//            make_triple(trace_obj, onto.exec_time, 0)  # set to 0 so next is 1
            result.add(new BackendFactEntity(
                    "owl:NamedIndividual", trace,
                    "depth",
                    "xsd:int", "0"
            ));
            result.add(new BackendFactEntity(
                    "owl:NamedIndividual", trace,
                    "in_trace",
                    "owl:NamedIndividual", trace
            ));
//            make_triple(trace_obj, onto.index, 0)
//            make_triple(trace_obj, onto.student_index, 0)
//            make_triple(trace_obj, onto.depth, 0)  # set to 0 so next is 1
//            make_triple(trace_obj, onto.in_trace, trace_obj)  # each act

            QuestionEntity q = responses.get(0).getLeftAnswerObject().getQuestion(); // assume that list is never empty
            // iterate responses and make acts
            int student_index = 0;
            int maxId = 100;
            HashMap<String, Pair<String, Integer>> id2exprName = new HashMap<>();
            String prevActIRI = trace;
            for (ResponseEntity response : responses) {
                String[] actInfo = response.getLeftAnswerObject().getDomainInfo().split(":");
                assert(actInfo.length == 2);
                String phase = actInfo[0];
                String exId = actInfo[1];
//              int exId = Integer.parseInt(actInfo[1]);

                String act_iri_t = exId + "_" + exId;

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
                            Pair<String, Integer> pair = id2exprName.get(exId);
                            exprName = pair.getLeft();
                            execCount = pair.getRight();
                        } else {
                            exprName = getExpressionNameById(q, Integer.parseInt(exId));
                            execCount = 0; // set anyway, even not an expression
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
                    "owl:NamedIndividual", actIRI,
                    "student_next",
                    "owl:NamedIndividual", prevActIRI
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

        List<String> errorClasses = VOCAB.classDescendants("Erroneous");


        // filter facts and fill mistakes list ...
        for (BackendFactEntity violation : violations) {

            // detect instances of Erroneous class
            if (violation.getVerb().equals("rdf:type") && errorClasses.contains(violation.getObject())) {
                ViolationEntity violationEntity = new ViolationEntity();
                violationEntity.setLawName(violation.getObject());
//                violationEntity.setExplanationTemplateInfo(Arrays.asList(
//                        new ExplanationTemplateInfoEntity() //"it has happened."
//                ));
                if (violationEntity.getLawName() != null) {
                    violationEntity.setViolationFacts(new ArrayList<>(Arrays.asList(
                            violation //,
//                        nameToText.get(violation.getObject()),
//                        nameToText.get(violation.getSubject()),
//                        nameToPos.get(violation.getObject()),
//                        nameToPos.get(violation.getSubject())
                    )));
                    mistakes.add(violationEntity);
                }
            }
        }

        result.violations = mistakes;
        result.correctlyAppliedLaws = new ArrayList<>(); // TODO

        ProcessSolutionResult processResult = processSolution(violations);
        result.CountCorrectOptions = processResult.CountCorrectOptions;
        result.IterationsLeft = processResult.IterationsLeft + (mistakes.isEmpty() ? 0 : 1);
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

    @Override
    public ProcessSolutionResult processSolution(List<BackendFactEntity> solution) {
        InterpretSentenceResult result = new InterpretSentenceResult();
        result.CountCorrectOptions = 1;

        // TODO: retrieve full solution path
        result.IterationsLeft = 11;
        return result;
    }

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        return null;
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
        d.VOCAB.classDescendants("Erroneous");
    }
}
