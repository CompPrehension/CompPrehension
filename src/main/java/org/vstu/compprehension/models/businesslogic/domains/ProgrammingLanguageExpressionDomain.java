package org.vstu.compprehension.models.businesslogic.domains;

import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.utils.HyperText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.vstu.compprehension.models.businesslogic.*;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static org.junit.jupiter.api.Assertions.*;

@Component
public class ProgrammingLanguageExpressionDomain extends Domain {
    static final String EVALUATION_ORDER_QUESTION_TYPE = "OrderOperators";
    static final String EVALUATION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderOperatorsSupplementary";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
    static final String LAWS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/programming-language-expression-domain-laws.json";
    static final String QUESTIONS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/programming-language-expression-domain-questions.json";

    public ProgrammingLanguageExpressionDomain() {
        name = "ProgrammingLanguageExpressionDomain";

        fillConcepts();
        readLaws(this.getClass().getClassLoader().getResourceAsStream(LAWS_CONFIG_PATH));
    }

    private void fillConcepts() {
        concepts = new ArrayList<>();

        Concept operandConcept = addConcept("operand");
        Concept simpleOperandConcept = addConcept("simple_operand");
        Concept operatorConcept = addConcept("operator", new ArrayList<>(Arrays.asList(operandConcept)));
        Concept variableConcept = addConcept("variable", new ArrayList<>(Arrays.asList(simpleOperandConcept)));
        Concept literalConcept = addConcept("literal", new ArrayList<>(Arrays.asList(simpleOperandConcept)));
        Concept precedenceConcept = addConcept("precedence");
        Concept associativityConcept = addConcept("associativity");
        Concept leftAssociativityConcept = addConcept("left_associativity", new ArrayList<>(Arrays.asList(associativityConcept)));
        Concept rightAssociativityConcept = addConcept("right_associativity", new ArrayList<>(Arrays.asList(associativityConcept)));
        Concept absentAssociativityConcept = addConcept("absent_associativity", new ArrayList<>(Arrays.asList(associativityConcept)));
        Concept arityConcept = addConcept("arity");
        Concept unaryConcept = addConcept("unary", new ArrayList<>(Arrays.asList(arityConcept)));
        Concept binaryConcept = addConcept("binary", new ArrayList<>(Arrays.asList(arityConcept)));
        Concept ternaryConcept = addConcept("ternary", new ArrayList<>(Arrays.asList(arityConcept)));
        Concept singleTokenOperatorConcept = addConcept("single_token");
        Concept twoTokenOperatorConcept = addConcept("two_token");
        Concept singleTokenUnaryConcept = addConcept("single_token_unary", new ArrayList<>(Arrays.asList(singleTokenOperatorConcept, unaryConcept)));
        Concept singleTokenBinaryConcept = addConcept("single_token_binary", new ArrayList<>(Arrays.asList(singleTokenOperatorConcept, binaryConcept)));
        Concept twoTokenUnaryConcept = addConcept("two_token_unary", new ArrayList<>(Arrays.asList(twoTokenOperatorConcept, unaryConcept)));
        Concept functionCallConcept = addConcept("function_call", new ArrayList<>(Arrays.asList(twoTokenUnaryConcept)));
        Concept twoTokenBinaryConcept = addConcept("two_token_binary", new ArrayList<>(Arrays.asList(twoTokenOperatorConcept, binaryConcept)));
        Concept subscriptConcept = addConcept("operator_[", new ArrayList<>(Arrays.asList(twoTokenBinaryConcept)));
        Concept twoTokenTernaryConcept = addConcept("two_token_ternary", new ArrayList<>(Arrays.asList(twoTokenOperatorConcept, binaryConcept)));
        Concept operatorEvaluationStateConcept = addConcept("operator_evaluation_state");
        Concept operatorEvaluatingLeftOperandFirstConcept = addConcept("operator_evaluating_left_operand_first", new ArrayList<>(Arrays.asList(binaryConcept, operatorEvaluationStateConcept)));
        Concept operatorUnaryPlusConcept = addConcept("operator_unary_+", new ArrayList<>(Arrays.asList(singleTokenUnaryConcept)));
        Concept operatorUnaryMinusConcept = addConcept("operator_unary_-", new ArrayList<>(Arrays.asList(singleTokenUnaryConcept)));
        Concept operatorUnaryPtrConcept = addConcept("operator_unary_*", new ArrayList<>(Arrays.asList(singleTokenUnaryConcept)));
        Concept operatorBinaryPlusConcept = addConcept("operator_binary_+", new ArrayList<>(Arrays.asList(singleTokenBinaryConcept)));
        Concept operatorBinaryMinusConcept = addConcept("operator_binary_-", new ArrayList<>(Arrays.asList(singleTokenBinaryConcept)));
        Concept operatorBinaryMultipleConcept = addConcept("operator_binary_*", new ArrayList<>(Arrays.asList(singleTokenBinaryConcept)));
        Concept operatorBinaryCommaConcept = addConcept("operator_binary_,", new ArrayList<>(Arrays.asList(singleTokenBinaryConcept)));
        Concept operatorTernaryConcept = addConcept("operator_ternary", new ArrayList<>(Arrays.asList(twoTokenTernaryConcept, operatorEvaluatingLeftOperandFirstConcept)));
        Concept operatorEqualsConcept = addConcept("operator_==", new ArrayList<>(Arrays.asList(singleTokenBinaryConcept)));
        Concept prefixOperatorConcept = addConcept("prefix", new ArrayList<>(Arrays.asList(unaryConcept)));
        Concept postfixOperatorConcept = addConcept("postfix", new ArrayList<>(Arrays.asList(unaryConcept)));
        Concept operatorPrefixIncrementConcept = addConcept("operator_prefix_++", new ArrayList<>(Arrays.asList(singleTokenUnaryConcept, prefixOperatorConcept)));
        Concept operatorPrefixDecrementConcept = addConcept("operator_prefix_--", new ArrayList<>(Arrays.asList(singleTokenUnaryConcept, prefixOperatorConcept)));
        Concept operatorPostfixIncrementConcept = addConcept("operator_postfix_++", new ArrayList<>(Arrays.asList(singleTokenUnaryConcept, postfixOperatorConcept)));
        Concept operatorPostfixDecrementConcept = addConcept("operator_postfix_--", new ArrayList<>(Arrays.asList(singleTokenUnaryConcept, postfixOperatorConcept)));
        Concept typeConcept = addConcept("type");
        Concept systemIntegrationTestConcept = addConcept("SystemIntegrationTest");
    }

    private Concept addConcept(String name, List<Concept> baseConcepts) {
        Concept concept = new Concept(name, baseConcepts);
        concepts.add(concept);
        return concept;
    }

    private Concept addConcept(String name) {
        Concept concept = new Concept(name);
        concepts.add(concept);
        return concept;
    }

    private void readLaws(InputStream inputStream) {
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
    public List<HyperText> getFullSolutionTrace(Question question) {
    	return null;
    }

    @Override
    public void update() {
    }

    @Override
    public ExerciseForm getExerciseForm() {
        return null;
    }

    @Override
    public ExerciseEntity processExerciseForm(ExerciseForm ef) {
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

    static List<Question> QUESTIONS;
    @Override
    protected List<Question> getQuestionTemplates() {
        if (QUESTIONS == null) {
            QUESTIONS = readQuestions(this.getClass().getClassLoader().getResourceAsStream(QUESTIONS_CONFIG_PATH));
        }
        return QUESTIONS;
    }

    Question makeQuestionCopy(Question q, ExerciseAttemptEntity exerciseAttemptEntity) {
        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showTrace(false)
                .multipleSelectionEnabled(true)
                .orderNumberOptions(new OrderQuestionOptionsEntity.OrderNumberOptions("/", OrderQuestionOptionsEntity.OrderNumberPosition.SUFFIX, null))
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

        List<String> tokens = new ArrayList<>();
        for (BackendFactEntity fact : q.getStatementFacts()) {
            tokens.add(fact.getObject());
        }
        entity.setStatementFacts(getBackendFacts(tokens));
        entity.setQuestionType(q.getQuestionType());


        switch (q.getQuestionType()) {
            case ORDER:
                entity.setQuestionText(ExpressionToHtml(q.getQuestionText().getText()));
                entity.setOptions(orderQuestionOptions);
                return new Ordering(entity);
            case MATCHING:
                entity.setQuestionText(QuestionTextToHtml(q.getQuestionText().getText()));
                entity.setOptions(matchingQuestionOptions);
                return new Matching(entity);
            case MULTI_CHOICE:
                entity.setQuestionText(QuestionTextToHtml(q.getQuestionText().getText()));
                entity.setOptions(multiChoiceQuestionOptions);
                return new MultiChoice(entity);
            default:
                throw new UnsupportedOperationException("Unknown type in ProgrammingLanguageExpressionDomain::makeQuestion: " + q.getQuestionType());
        }
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
        if (res != null) {
            return makeQuestionCopy(res, questionRequest.getExerciseAttempt());
        }

        // make a SingleChoice question ...
        QuestionEntity question = new QuestionEntity();
        question.setExerciseAttempt(questionRequest.getExerciseAttempt());
        question.setQuestionText("Choose associativity of operator binary +");
        question.setQuestionType(QuestionType.SINGLE_CHOICE);
        question.setQuestionDomainType("ChooseAssociativity");
        question.setAreAnswersRequireContext(true);
        question.setAnswerObjects(new ArrayList<>(Arrays.asList(
                createAnswerObject(question, "left", "left_associativity", "left", true),
                createAnswerObject(question, "right", "right_associativity", "right", true),
                createAnswerObject(question, "no associativity", "absent_associativity", "no associativity", true)
        )));
        return new SingleChoice(question);
    }

    private AnswerObjectEntity createAnswerObject(QuestionEntity question, String text, String concept, String domainInfo, boolean isLeft) {
        AnswerObjectEntity answerObject = new AnswerObjectEntity();
        answerObject.setHyperText(text);
        answerObject.setRightCol(!isLeft);
        answerObject.setDomainInfo(domainInfo);
        answerObject.setConcept(concept);
        answerObject.setQuestion(question);
        return answerObject;
    }

    public static String ExpressionToHtml(String text) {
        StringBuilder sb = new StringBuilder(text);

        // Wrap every token of the expression with <span> and some metadata ...
        Pattern pattern = Pattern.compile("\\<\\=|\\>\\=|\\=\\=|\\!\\=|\\<\\<|\\>\\>|\\+|\\-|\\*|\\/|\\<|\\>|\\w+");
        Matcher matcher = pattern.matcher(sb.toString());
        int idx = 0;
        int anwerIdx = -1;
        int offset = 0;
        while (offset < sb.length() && matcher.find(offset)) {
            String match = matcher.group(0);
            String replaceStr = match.matches("\\w")
                    ? "<span data-comp-ph-pos='" + (++idx) +"' class='comp-ph-expr-const'>" + matcher.group(0) +"</span>"
                    : "<span data-comp-ph-pos='" + (++idx) +"' id='answer_" + (++anwerIdx) +"' class='comp-ph-expr-op-btn'>" + matcher.group(0) +"</span>";

            sb.replace(matcher.start(), matcher.end(), replaceStr);
            offset = matcher.start() + replaceStr.length() ;
            matcher = pattern.matcher(sb.toString());
        }

        sb.insert(0, "<p class='comp-ph-expr'>");
        sb.append("<!-- Original expression: " + text + "-->");
        sb.append("</p>");
        return QuestionTextToHtml(sb.toString());
    }

    public static String QuestionTextToHtml(String text) {
        StringBuilder sb = new StringBuilder(text
                .replaceAll("\\*", "&#8727")
                .replaceAll("\\n", "<br>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;"));
        sb.insert(0, "<div class='comp-ph-question'>"); sb.append("</div>");
        return sb.toString();
    }

    private String getName(int step, int index) {
        return "op__" + step + "__" + index;
    }

    public List<BackendFactEntity> getBackendFacts(List<String> expression) {
        List<BackendFactEntity> facts = new ArrayList<>();
        int index = 0;
        for (String token : expression) {
            index++;
            for (int step = 0; step <= expression.size(); ++step) {
                String name = getName(step, index);
                facts.add(new BackendFactEntity(name, "rdf:type", "owl:NamedIndividual"));
                facts.add(new BackendFactEntity("owl:NamedIndividual", name, "index", "xsd:int", String.valueOf(index)));
                facts.add(new BackendFactEntity("owl:NamedIndividual", name, "step", "xsd:int", String.valueOf(step)));
            }
            facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "text", "xsd:string", token));
        }
        facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "last", "xsd:boolean", "true"));
        return facts;
    }

    // filter positive laws by question type and tags
    @Override
    public List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE) || questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
            List<PositiveLaw> positiveLaws = new ArrayList<>();
            for (PositiveLaw law : getPositiveLaws()) {
                boolean needLaw = true;
                for (Tag tag : law.getTags()) {
                    boolean inQuestionTags = false;
                    for (Tag questionTag : tags) {
                        if (questionTag.getName().equals(tag.getName())) {
                            inQuestionTags = true;
                        }
                    }
                    if (!inQuestionTags) {
                        needLaw = false;
                    }
                }
                if (needLaw) {
                    positiveLaws.add(law);
                }
            }
            return positiveLaws;
        }
        return new ArrayList<>(Arrays.asList());
    }

    public List<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            List<NegativeLaw> negativeLaws = new ArrayList<>();
            for (NegativeLaw law : getNegativeLaws()) {
                boolean needLaw = true;
                //filter by tags after separation
                if (needLaw) {
                    negativeLaws.add(law);
                }
            }
            return negativeLaws;
        }
        return new ArrayList<>(Arrays.asList());
    }

    public List<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "has_operand",
                    "before_direct",
                    "before_by_third_operator",
                    "before_third_operator",
                    "before_as_operand",
                    "is_operator_with_strict_operands_order",
                    "high_precedence_diff_precedence",
                    "high_precedence_left_assoc",
                    "high_precedence_right_assoc",
                    "is_operand",
                    "law_name"
            ));
        } else if (questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "get_type"
            ));
        }
        return new ArrayList<>();
    }

    public List<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "student_error_more_precedence",
                    "student_error_left_assoc",
                    "student_error_right_assoc",
                    "student_error_in_complex",
                    "student_error_strict_operands_order",
                    "student_error_more_precedence_base",
                    "student_error_left_assoc_base",
                    "student_error_right_assoc_base",
                    "student_error_in_complex_base",
                    "student_error_strict_operands_order_base",
                    "text",
                    "index",
                    "before_direct",
                    "student_pos_number",
                    "is_operand"
            ));
        } else if (questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "wrong_type"
            ));
        }
        return new ArrayList<>();
    }

    @Override
    public List<BackendFactEntity> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            List<BackendFactEntity> result = new ArrayList<>();
            int pos = 1;
            HashSet<String> used = new HashSet<>();
            for (ResponseEntity response : responses) {
                result.add(new BackendFactEntity(
                        "owl:NamedIndividual",
                        response.getLeftAnswerObject().getDomainInfo(),
                        "student_pos_number",
                        "xsd:int",
                        String.valueOf(pos)
                ));
                for (String earlier : used) {
                    result.add(new BackendFactEntity(
                            "owl:NamedIndividual",
                            earlier,
                            "student_pos_less",
                            "owl:NamedIndividual",
                            response.getLeftAnswerObject().getDomainInfo()
                    ));
                }
                used.add(response.getLeftAnswerObject().getDomainInfo());
                pos = pos + 1;
            }

            for (AnswerObjectEntity answerObject : answerObjects) {
                if (!used.contains(answerObject.getDomainInfo())) {
                    for (String earlier : used) {
                        result.add(new BackendFactEntity(
                                "owl:NamedIndividual",
                                earlier,
                                "student_pos_less",
                                "owl:NamedIndividual",
                                answerObject.getDomainInfo()
                        ));
                    }
                }
            }
            return result;
        } else if (questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
            List<BackendFactEntity> result = new ArrayList<>();
            for (ResponseEntity response : responses) {
                result.add(new BackendFactEntity(
                        "owl:NamedIndividual",
                        response.getLeftAnswerObject().getDomainInfo(),
                        "student_type",
                        "xsd:string",
                        response.getRightAnswerObject().getDomainInfo()
                ));
            }
            return result;
        }
        return new ArrayList<>();
    }

    private static Optional<Integer> getIndexFromName(String name, boolean allowNotZeroStep) {
        Assertions.assertTrue(name.startsWith("op__"), name);
        String[] parts = name.split("__");
        assertEquals(3, parts.length, name);
        if (allowNotZeroStep || parts[1].equals("0")) {
            return Optional.of(Integer.parseInt(parts[2]));
        }
        return Optional.empty();
    }

    class CorrectAnswerImpl {
        String domainID;
        String lawName;
    }

    private List<CorrectAnswerImpl> getCorrectAnswers(List<BackendFactEntity> solution) {
        Map<String, List<String>> before = new HashMap<>();
        Map<String, String> studentPos = new HashMap<>();
        Map<String, String> operatorLawName = new HashMap<>();
        HashSet<String> isOperand = new HashSet<>();
        HashSet<String> allTokens = new HashSet<>();

        List<CorrectAnswerImpl> result = new ArrayList<>();
        for (BackendFactEntity fact : solution) {
            if (fact.getVerb().equals("before_direct")) {
                if (!before.containsKey(fact.getObject())) {
                    before.put(fact.getObject(), new ArrayList<>());
                }
                before.get(fact.getObject()).add(fact.getSubject());
                allTokens.add(fact.getObject());
                allTokens.add(fact.getSubject());
            } else if (fact.getVerb().equals("student_pos_number")) {
                studentPos.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("is_operand")) {
                isOperand.add(fact.getSubject());
            } else if (fact.getVerb().equals("law_name")) {
                operatorLawName.put(fact.getSubject(), fact.getObject());
            }
        }

        for (String operator : allTokens) {
            if (!operator.startsWith("op__0") || isOperand.contains(operator)) {
                continue;
            }
            boolean can = !studentPos.containsKey(operator);
            if (before.containsKey(operator)) {
                List<String> deps = before.get(operator);
                for (String dep : deps) {
                    if (!studentPos.containsKey(dep) && !isOperand.contains(dep)) {
                        can = false;
                        break;
                    }
                }
            }
            if (can) {
                CorrectAnswerImpl answer = new CorrectAnswerImpl();
                answer.domainID = operator;
                answer.lawName = operatorLawName.get(operator);
                result.add(answer);
            }
        }
        return result;
    }

    @Override
    public ProcessSolutionResult processSolution(List<BackendFactEntity> solution) {
        Map<String, String> studentPos = new HashMap<>();
        HashSet<String> isOperand = new HashSet<>();
        HashSet<String> allTokens = new HashSet<>();
        for (BackendFactEntity fact : solution) {
            if (fact.getVerb().equals("before_direct")) {
                allTokens.add(fact.getObject());
                allTokens.add(fact.getSubject());
            } else if (fact.getVerb().equals("student_pos_number")) {
                studentPos.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("is_operand")) {
                isOperand.add(fact.getSubject());
            }
        }

        int IterationsLeft = 0;
        for (String operator : allTokens) {
            if (operator.startsWith("op__0") && !isOperand.contains(operator) && !studentPos.containsKey(operator)) {
                IterationsLeft++;
            }
        }

        InterpretSentenceResult result = new InterpretSentenceResult();
        result.CountCorrectOptions = getCorrectAnswers(solution).size();
        result.IterationsLeft = IterationsLeft;
        return result;
    }

    HyperText getCorrectExplanation(String lawName) {
        return new HyperText(lawName);
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

        val solution = q.getSolutionFacts();
        lastCorrectInteraction.ifPresent(i -> solution.addAll(responseToFacts(q.getQuestionDomainType(), i.getResponses(), q.getAnswerObjects())));

        List<CorrectAnswerImpl> correctAnswerImpls = getCorrectAnswers(solution);
        for (AnswerObjectEntity answer : q.getAnswerObjects()) {
            for (CorrectAnswerImpl answerImpl : correctAnswerImpls) {
                if (answerImpl.domainID.equals(answer.getDomainInfo())) {
                    val answers = new ArrayList<>(lastCorrectInteractionAnswers);
                    answers.add(Pair.of(answer, answer));

                    CorrectAnswer correctAnswer = new CorrectAnswer();
                    correctAnswer.question = q.getQuestionData();
                    correctAnswer.answers = answers;
                    correctAnswer.lawName = answerImpl.lawName;
                    correctAnswer.explanation = getCorrectExplanation(answerImpl.lawName);
                    return correctAnswer;
                }
            }
        }
        return null;
    }

    @Override
    public Question makeSupplementaryQuestion(InterpretSentenceResult interpretSentenceResult, ExerciseAttemptEntity exerciseAttemptEntity) {
        assertFalse(interpretSentenceResult.violations.isEmpty());
        HashSet<String> targetConcepts = new HashSet<>();
        targetConcepts.add(interpretSentenceResult.violations.get(0).getLawName());
        targetConcepts.add("supplementary");

        Question res = findQuestion(exerciseAttemptEntity.getExercise().getTags(), targetConcepts, new HashSet<>(), new HashSet<>(), new HashSet<>());
        if (res != null) {
            return makeQuestionCopy(res, exerciseAttemptEntity);
        }
        return null;
    }

    @Override
    public InterpretSentenceResult interpretSentence(List<BackendFactEntity> violations) {
        List<ViolationEntity> mistakes = new ArrayList<>();

        // retrieve subjects' info from facts ...
        Map<String, BackendFactEntity> nameToText = new HashMap<>();
        Map<String, BackendFactEntity> nameToPos = new HashMap<>();

        for (BackendFactEntity violation : violations) {
            if (violation.getVerb().equals("text")) {
                nameToText.put(violation.getSubject(), violation);
            } else if (violation.getVerb().equals("index")) {
                nameToPos.put(violation.getSubject(), violation);
            }
        }

        // filter facts and fill mistakes list ...
        for (BackendFactEntity violation : violations) {
            ViolationEntity violationEntity = new ViolationEntity();
            if (violation.getVerb().equals("student_error_more_precedence")) {
                if (getIndexFromName(violation.getSubject(), false).orElse(0) > getIndexFromName(violation.getObject(), false).orElse(0)) {
                    violationEntity.setLawName("error_single_token_binary_operator_has_unevaluated_higher_precedence_left");
                } else {
                    violationEntity.setLawName("error_single_token_binary_operator_has_unevaluated_higher_precedence_right");
                }
            } else if (violation.getVerb().equals("student_error_left_assoc")) {
                violationEntity.setLawName("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left");
            } else if (violation.getVerb().equals("student_error_right_assoc")) {
                violationEntity.setLawName("error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right");
            } else if (violation.getVerb().equals("wrong_type")) {
                violationEntity.setLawName("error_wrong_type");
            }
            if (violationEntity.getLawName() != null) {
                violationEntity.setViolationFacts(new ArrayList<>(Arrays.asList(
                        violation,
                        nameToText.get(violation.getObject()),
                        nameToText.get(violation.getSubject()),
                        nameToPos.get(violation.getObject()),
                        nameToPos.get(violation.getSubject())
                )));
                mistakes.add(violationEntity);
            }
        }

        InterpretSentenceResult result = new InterpretSentenceResult();
        result.violations = mistakes;
        result.correctlyAppliedLaws = calculateCorrectlyAppliedLaws(violations);

        ProcessSolutionResult processResult = processSolution(violations);
        result.CountCorrectOptions = processResult.CountCorrectOptions;
        result.IterationsLeft = processResult.IterationsLeft + (mistakes.isEmpty() ? 0 : 1);
        return result;
    }

    List<String> calculateCorrectlyAppliedLaws(List<BackendFactEntity> violations) {
        List<String> result = new ArrayList<>();

        Map<String, Integer> nameToStudentPos = new HashMap<>();
        Integer maxStudentPos = -1;
        for (BackendFactEntity violation : violations) {
            if (violation.getVerb().equals("student_pos_number")) {
                Integer studentPosNumber = Integer.parseInt(violation.getObject());
                maxStudentPos = max(maxStudentPos, studentPosNumber);
                nameToStudentPos.put(violation.getSubject(), studentPosNumber);
            }
        }

        for (BackendFactEntity violation : violations) {
            // Consider only errors that could happen at current step and where current token will be error reason
            if (!nameToStudentPos.getOrDefault(violation.getObject(), -2).equals(maxStudentPos) ||
                    nameToStudentPos.containsKey(violation.getSubject())) {
                continue;
            }
            String correctlyAppliedLaw = null;
            if (violation.getVerb().equals("student_error_more_precedence_base")) {
                if (getIndexFromName(violation.getSubject(), false).orElse(0) > getIndexFromName(violation.getObject(), false).orElse(0)) {
                    correctlyAppliedLaw = "error_single_token_binary_operator_has_unevaluated_higher_precedence_left";
                } else {
                    correctlyAppliedLaw = "error_single_token_binary_operator_has_unevaluated_higher_precedence_right";
                }
            } else if (violation.getVerb().equals("student_error_left_assoc_base")) {
                correctlyAppliedLaw = "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left";
            } else if (violation.getVerb().equals("student_error_right_assoc_base")) {
                correctlyAppliedLaw = "error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right";
            }
            if (correctlyAppliedLaw != null) {
                result.add(correctlyAppliedLaw);
            }
        }

        return result;
    }

    @Override
    public ArrayList<HyperText> makeExplanation(List<ViolationEntity> mistakes, FeedbackType feedbackType) {
        ArrayList<HyperText> result = new ArrayList<>();
        for (ViolationEntity mistake : mistakes) {
            result.add(makeExplanation(mistake, feedbackType));
        }
        return result;
    }

    private static String getOperatorTextDescription(String errorText) {
        if (errorText.equals("(")) {
            return "parenthesis ";
        } else if (errorText.equals("[")) {
            return "brackets ";
        } else if (errorText.contains("(")) {
            return "function call ";
        }
        return "operator ";
    }

    private HyperText makeExplanation(ViolationEntity mistake, FeedbackType feedbackType) {

        // retrieve subjects' info from facts, and find base and third ...
        BackendFactEntity base = null;
        BackendFactEntity third = null;
        Map<String, String> nameToText = new HashMap<>();
        Map<String, String> nameToPos = new HashMap<>();
        for (BackendFactEntity fact : mistake.getViolationFacts()) {
            if (fact.getVerb().equals("before_third_operator")) {
                third = fact;
            } else if (fact.getVerb().equals("index")) {
                nameToPos.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("text")) {
                nameToText.put(fact.getSubject(), fact.getObject());
            } else {
                base = fact;
            }
        }

        String errorText = nameToText.get(base.getSubject());
        String reasonText = nameToText.get(base.getObject());

        String thirdOperatorPos = third == null ? "" : nameToPos.get(third.getObject());
        String thirdOperatorText = third == null ? "" : nameToText.get(third.getObject());

        String reasonPos = nameToPos.get(base.getObject());
        String errorPos = nameToPos.get(base.getSubject());

        String what = getOperatorTextDescription(reasonText) + reasonText + " on pos " + reasonPos
                + " should be evaluated before " + getOperatorTextDescription(errorText) + errorText + " on pos " + errorPos;
        String reason = "";

        String errorType = mistake.getLawName();

        if (errorType.equals("error_single_token_binary_operator_has_unevaluated_higher_precedence_left") ||
                errorType.equals("error_single_token_binary_operator_has_unevaluated_higher_precedence_right")) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has higher precedence";
        } else if (errorType.equals("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left") && errorText.equals(reasonText)) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has left associativity and is evaluated from left to right";
        } else if (errorType.equals("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left")) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has the same precedence and left associativity";
        } else if (errorType.equals("error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right") && errorText.equals(reasonText)) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has right associativity and is evaluated from right to left";
        } else if (errorType.equals("error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right")) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has the same precedence and right associativity";
//        } else if (error.Type == StudentErrorType.IN_COMPLEX && errorText.equals("(")) {
//            reason = " because function arguments are evaluated before function call​";
//        } else if (error.Type == StudentErrorType.IN_COMPLEX && errorText.equals("[")) {
//            reason = " because expression in brackets is evaluated before brackets";
//        } else if (error.Type == StudentErrorType.IN_COMPLEX && thirdOperatorText.equals("(")) {
//            reason = " because expression in parenthesis is evaluated before operators​ outside of them";
//        } else if (error.Type == StudentErrorType.IN_COMPLEX && thirdOperatorText.equals("[")) {
//            reason = " because expression in brackets is evaluated before operator outside of them​​";
//        } else if (error.Type == StudentErrorType.STRICT_OPERANDS_ORDER) {
//            reason = " because the left operand of the " + getOperatorTextDescription(thirdOperatorText) + thirdOperatorText + " on pos " + thirdOperatorPos + " must be evaluated before its right operand​";
        } else {
            reason = " because unknown error";
        }

        return new HyperText(what + "\n" + reason);
    }
}
