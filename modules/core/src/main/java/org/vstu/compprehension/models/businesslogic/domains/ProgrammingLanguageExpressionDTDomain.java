package org.vstu.compprehension.models.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import its.model.DomainSolvingModel;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.util.HtmlUtils;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph;
import org.vstu.compprehension.models.businesslogic.domains.helpers.ProgrammingLanguageExpressionRDFTransformer;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.models.entities.QuestionOptions.*;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain.NS_code;

@Log4j2
public class ProgrammingLanguageExpressionDTDomain extends Domain {
    static final String EVALUATION_ORDER_QUESTION_TYPE = "OrderOperators";
    static final String EVALUATION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderOperatorsSupplementary";
    static final String OPERANDS_TYPE_QUESTION_TYPE = "OperandsType";
    static final String PRECEDENCE_TYPE_QUESTION_TYPE = "PrecedenceType";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
    static final String RESOURCES_LOCATION = "org/vstu/compprehension/models/businesslogic/domains/";
    static final String LAWS_CONFIG_PATH = RESOURCES_LOCATION + "programming-language-expression-domain-laws.json";
    static final String QUESTIONS_CONFIG_PATH = RESOURCES_LOCATION + "programming-language-expression-domain-questions.json";
    static final String SUPPLEMENTARY_CONFIG_PATH = RESOURCES_LOCATION + "programming-language-expression-domain-supplementary-strategy.json";
    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "programming-language-expression-domain-dt-messages";
    
    static final String MESSAGE_PREFIX = "expr_domain_dt.";
    static final String SUPPLEMENTARY_PREFIX = "supplementary.";
    
    public static final String END_EVALUATION = "student_end_evaluation";
    private final LocalizationService localizationService;
    protected final QuestionBank qMetaStorage;
    private static final HashMap<String, Tag> tags = new HashMap<>() {{
        put("C++", new Tag("C++", 1L));  	// (2 ^ 0)
        put("basics", new Tag("basics", 2L));  	// (2 ^ 1)
        put("errors", new Tag("errors", 4L));  	// (2 ^ 2)
        put("evaluation", new Tag("evaluation", 8L));  	// (2 ^ 3)
        put("operators", new Tag("operators", 16L));  	// (2 ^ 4)
        put("order", new Tag("order", 32L));  	// (2 ^ 5)
    }};
    
    @Override
    public String getDBShortName() {
        return "expression"; //
    }
    
    @Override
    public String getSolvingBackendId() {
        return DecisionTreeReasonerBackend.BACKEND_ID;
    }
    
    @SneakyThrows
    public ProgrammingLanguageExpressionDTDomain(
            DomainEntity domainEntity,
            LocalizationService localizationService,
            RandomProvider randomProvider,
            QuestionBank qMetaStorage) {
        
        super(domainEntity, randomProvider);
        
        this.localizationService = localizationService;
        this.qMetaStorage = qMetaStorage;
        
        fillConcepts();
        readLaws(this.getClass().getClassLoader().getResourceAsStream(LAWS_CONFIG_PATH));
        //LOOK readSupplementaryConfig(this.getClass().getClassLoader().getResourceAsStream(SUPPLEMENTARY_CONFIG_PATH));
    }
    
    private static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "programming-language-expression-domain-model/";
    private final DomainSolvingModel domainSolvingModel = new DomainSolvingModel(
            this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION), //FIXME
            DomainSolvingModel.BuildMethod.LOQI
    );

    @NotNull
    @Override
    public String getDisplayName(Language language) {
        return localizationService.getMessage("expr_domain_dt.display_name", language);
    }

    @Nullable
    @Override
    public String getDescription(Language language) {
        return localizationService.getMessage("expr_domain_dt.description", language);
    }

    @NotNull
    @Override
    public Map<String, Tag> getTags() {
        return tags;
    }
    
    //-----------КОНЦЕПТЫ---------
    
    private void fillConcepts() {
        concepts = new HashMap<>();
        
        int flags = Concept.FLAG_VISIBLE_TO_TEACHER | Concept.FLAG_TARGET_ENABLED;
        int invisible = Concept.FLAG_TARGET_ENABLED;
        int noFlags = Concept.DEFAULT_FLAGS;
        
        Concept operandConcept = addConcept("operand");
        Concept simpleOperandConcept = addConcept("simple_operand");
        Concept operatorConcept = addConcept("operator", List.of(operandConcept));
        Concept variableConcept = addConcept("variable", List.of(simpleOperandConcept));
        Concept literalConcept = addConcept("literal", List.of(simpleOperandConcept));
        Concept precedenceConcept = addConcept("precedence");
        Concept associativityConcept = addConcept("associativity");
        Concept leftAssociativityConcept = addConcept("left_associativity", List.of(associativityConcept));
        Concept rightAssociativityConcept = addConcept("right_associativity", List.of(associativityConcept));
        Concept assignConcept = addConcept("operator_=", List.of(rightAssociativityConcept), "Оператор присваивания", flags);
        Concept absentAssociativityConcept = addConcept("absent_associativity", List.of(associativityConcept));
        Concept arityConcept = addConcept("arity");
        Concept unaryConcept = addConcept("unary", List.of(arityConcept), "Унарные операции", invisible);
        Concept binaryConcept = addConcept("binary", List.of(arityConcept), "Бинарные операции", invisible);
        Concept ternaryConcept = addConcept("ternary", List.of(arityConcept));
        Concept singleTokenOperatorConcept = addConcept("single_token");
        Concept twoTokenOperatorConcept = addConcept("two_token");
        Concept singleTokenUnaryConcept = addConcept("single_token_unary", List.of(singleTokenOperatorConcept, unaryConcept));
        Concept singleTokenBinaryConcept = addConcept("single_token_binary", List.of(singleTokenOperatorConcept, binaryConcept));
        Concept twoTokenUnaryConcept = addConcept("two_token_unary", List.of(twoTokenOperatorConcept, unaryConcept));
        Concept twoTokenBinaryConcept = addConcept("two_token_binary", List.of(twoTokenOperatorConcept, binaryConcept));
        Concept twoTokenTernaryConcept = addConcept("two_token_ternary", List.of(twoTokenOperatorConcept, binaryConcept, ternaryConcept));
        Concept operatorEvaluationStateConcept = addConcept("operator_evaluation_state");
        Concept operatorEvaluatingLeftOperandFirstConcept = addConcept("operator_evaluating_left_operand_first", List.of(binaryConcept, operatorEvaluationStateConcept));
        
        Concept arithmetics = addConcept("arithmetics", List.of(), "Арифметические операции", flags);
        Concept operatorBinaryPlusConcept = addConcept("operator_binary_+", List.of(singleTokenBinaryConcept, arithmetics), "x + y", invisible);
        Concept operatorBinaryMinusConcept = addConcept("operator_binary_-", List.of(singleTokenBinaryConcept, arithmetics), "x - y", invisible);
        Concept operatorBinaryMultipleConcept = addConcept("operator_binary_*", List.of(singleTokenBinaryConcept, arithmetics), "x * y", invisible);
        Concept operatorBinaryDivideConcept = addConcept("operator_/", List.of(singleTokenBinaryConcept, arithmetics), "x / y", invisible);
        Concept operatorUnaryPlusConcept = addConcept("operator_unary_+", List.of(singleTokenUnaryConcept, arithmetics), "+z", invisible);
        Concept operatorUnaryMinusConcept = addConcept("operator_unary_-", List.of(singleTokenUnaryConcept, arithmetics), "-z", invisible);
        // Concept operatorBinaryDivideIntConcept = addConcept("operator_//", List.of(singleTokenBinaryConcept, arithmetics), "x // y", invisible);  // Python only
        // Concept operatorMatMulConcept = addConcept("operator_@", List.of(singleTokenBinaryConcept, arithmetics), "x @ y", invisible);  // Python only
        
        Concept incrementConcept = addConcept("increment", List.of(unaryConcept), "Инкремент и декремент", flags);
        Concept prefixOperatorConcept = addConcept("prefix", List.of(incrementConcept));
        Concept postfixOperatorConcept = addConcept("postfix", List.of(incrementConcept));
        Concept operatorPrefixIncrementConcept = addConcept("operator_prefix_++", List.of(singleTokenUnaryConcept, prefixOperatorConcept), "++z", invisible);
        Concept operatorPrefixDecrementConcept = addConcept("operator_prefix_--", List.of(singleTokenUnaryConcept, prefixOperatorConcept), "--z", invisible);
        Concept operatorPostfixIncrementConcept = addConcept("operator_postfix_++", List.of(singleTokenUnaryConcept, postfixOperatorConcept), "z++", invisible);
        Concept operatorPostfixDecrementConcept = addConcept("operator_postfix_--", List.of(singleTokenUnaryConcept, postfixOperatorConcept), "z--", invisible);
        
        
        Concept aug_assignments = addConcept("aug_assignments", List.of(singleTokenBinaryConcept), "Присваивания с обновлением", flags);
        addConcept("operator_+=", List.of(aug_assignments), "a += b", invisible);
        addConcept("operator_-=", List.of(aug_assignments), "a -= b", invisible);
        addConcept("operator_*=", List.of(aug_assignments), "a *= b", invisible);
        addConcept("operator_/=", List.of(aug_assignments), "a /= b", invisible);
        addConcept("operator_%=", List.of(aug_assignments), "a %= b", invisible);
        addConcept("operator_&=", List.of(aug_assignments), "a &= b", invisible);
        addConcept("operator_|=", List.of(aug_assignments), "a |= b", invisible);
        addConcept("operator_^=", List.of(aug_assignments), "a ^= b", invisible);
        addConcept("operator_<<=",List.of(aug_assignments), "a <<= b", invisible);
        addConcept("operator_>>=",List.of(aug_assignments), "a >>= b", invisible);
        // addConcept("operator_:=", List.of(aug_assignments), "a := b", invisible);  // Python only
        
        Concept comparison = addConcept("comparison", List.of(singleTokenBinaryConcept), "Операции сравнения", flags);
        Concept operatorEqualsConcept = addConcept("operator_==", List.of(comparison), "a == b", invisible);
        Concept operatorInequalConcept = addConcept("operator_!=", List.of(comparison), "a != b", invisible);
        Concept operatorLtConcept = addConcept("operator_<", List.of(comparison), "a < b", invisible);
        Concept operatorGtConcept = addConcept("operator_>", List.of(comparison), "a > b", invisible);
        Concept operatorLeConcept = addConcept("operator_<=", List.of(comparison), "a <= b", invisible);
        Concept operatorGeConcept = addConcept("operator_>=", List.of(comparison), "a >= b", invisible);
        Concept operatorEqConcept = addConcept("operator_<=>", List.of(comparison), "a <=> b", invisible);
        // Concept operatorIsConcept = addConcept("operator_is", List.of(comparison), "a is b", invisible);  // Python only
        
        Concept logical = addConcept("logical", List.of(), "Логические операции", flags);
        addConcept("operator_!", List.of(singleTokenUnaryConcept, logical), "!a", invisible);
        addConcept("operator_&&", List.of(singleTokenBinaryConcept, logical), "a && b", invisible);
        addConcept("operator_||", List.of(singleTokenBinaryConcept, logical), "a || b", invisible);
        // addConcept("operator_and", List.of(singleTokenBinaryConcept, logical), "a and b", invisible);  // Python only
        // addConcept("operator_or", List.of(singleTokenBinaryConcept, logical), "a or b", invisible);  // Python only
        
        Concept bitwise = addConcept("bitwise", List.of(), "Побитовые операции", flags);
        addConcept("operator_~", List.of(singleTokenUnaryConcept, bitwise), "~b", invisible);
        addConcept("operator_binary_&", List.of(singleTokenBinaryConcept, bitwise), "a & b", invisible);
        addConcept("operator_|", List.of(singleTokenBinaryConcept, bitwise), "a | b", invisible);
        addConcept("operator_^", List.of(singleTokenBinaryConcept, bitwise), "a ^ b", invisible);
        // see also stream_io below (duplicates under different category)
        addConcept("operator_>>", List.of(singleTokenBinaryConcept, bitwise), "a >> b", invisible);
        addConcept("operator_<<", List.of(singleTokenBinaryConcept, bitwise), "a << b", invisible);
        
        Concept arrays = addConcept("arrays", List.of(), "Массивы", noFlags);
        Concept subscriptConcept = addConcept("operator_subscript", List.of(twoTokenBinaryConcept, arrays), "Индексация массива a[i]", flags);
        
        Concept pointers = addConcept("pointers", List.of(singleTokenUnaryConcept), "Операции c указателями", flags);
        addConcept("operator_unary_*", List.of(pointers), "*ptr", invisible);
        addConcept("operator_&", List.of(pointers), "&val", invisible);
        
        Concept fieldAccess = addConcept("object_access", List.of(), "Обращение к полю", flags);
        addConcept("operator_.", List.of(singleTokenBinaryConcept, fieldAccess), "obj.field", invisible);
        addConcept("operator_->",List.of(singleTokenBinaryConcept, fieldAccess, pointers), "ptr->field", invisible);
        
        Concept functionCallConcept = addConcept("function_call", List.of(twoTokenUnaryConcept), "Вызов функции", flags);
        Concept functionCallConcept_2 = addConcept("operator_function_call", List.of(twoTokenUnaryConcept), "Вызов функции", flags);
        Concept operatorTernaryConcept = addConcept("operator_?", List.of(twoTokenTernaryConcept, operatorEvaluatingLeftOperandFirstConcept), "Тернарный оператор (?:)", invisible);  // c ? a : b
        
        Concept operatorBinaryCommaConcept = addConcept("operator_,", List.of(singleTokenBinaryConcept), "Запятая между выражениями", flags);
        
        Concept stream_io = addConcept("stream_io", List.of(), "Потоковый in/out", noFlags);
        addConcept("operator_>>", List.of(singleTokenBinaryConcept, stream_io), "in >> var", invisible);
        addConcept("operator_<<", List.of(singleTokenBinaryConcept, stream_io), "out << msg", invisible);
        
        // currently, absent in the data:
//        Concept namespace_static = addConcept("namespace_static", List.of(), "Пространство имён", noFlags);
//        addConcept("operator_:", List.of(singleTokenBinaryConcept, namespace_static), "Class:member", invisible);
//        addConcept("operator_::", List.of(singleTokenBinaryConcept, namespace_static), "space::member", invisible);
        
        Concept typeConcept = addConcept("type");
        Concept operandsTypeConcept = addConcept("operands_type");
        Concept precedenceTypeConcept = addConcept("precedence_type");
        Concept systemIntegrationTestConcept = addConcept("SystemIntegrationTest");
        Concept error = addConcept("error");
        Concept errorHigherPrecedenceLeft = addConcept("error_base_higher_precedence_left");
        Concept errorHigherPrecedenceRight = addConcept("error_base_higher_precedence_right");
        Concept errorSamePrecedenceLeftAssociativityLeft = addConcept("error_base_same_precedence_left_associativity_left");
        Concept errorSamePrecedenceRightAssociativityRight = addConcept("error_base_same_precedence_right_associativity_right");
        Concept errorInComplex = addConcept("error_base_student_error_in_complex");
        Concept errorStrictOperandsOrder = addConcept("error_base_student_error_strict_operands_order");
        Concept errorUnevaluatedOperand = addConcept("error_base_student_error_unevaluated_operand");
        Concept errorEarlyFinish = addConcept("error_base_student_error_early_finish");
        
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
    
    private Concept addConcept(String name, List<Concept> baseConcepts, String displayName, int flags) {
        Concept concept = new Concept(name, /*displayName,*/ baseConcepts, flags);
        return addConcept(concept);
    }
    
    private Concept addConcept(String name, List<Concept> baseConcepts) {
        Concept concept = new Concept(name, baseConcepts);
        return addConcept(concept);
    }
    
    private Concept addConcept(String name) {
        Concept concept = new Concept(name);
        return addConcept(concept);
    }
    
    //---------ЗАКОНЫ---------------
    
    private void readLaws(InputStream inputStream) {
        positiveLaws = new HashMap<>();
        negativeLaws = new HashMap<>();
        
        RuntimeTypeAdapterFactory<Law> runtimeTypeAdapterFactory =
                RuntimeTypeAdapterFactory
                        .of(Law.class, "positive")
                        .registerSubtype(PositiveLaw.class, "true")
                        .registerSubtype(NegativeLaw.class, "false");
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
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
        
        fillLawsTree();
        
        // assign mask bits to Laws
        var name2bit = _getLawsName2bit();
        for (Law t : positiveLaws.values()) {
            val name = t.getName();
            if (name2bit.containsKey(name)) {
                t.setBitmask(name2bit.get(name));
            }
        }
        name2bit = _getViolationsName2bit();
        for (Law t : negativeLaws.values()) {
            val name = t.getName();
            if (name2bit.containsKey(name)) {
                t.setBitmask(name2bit.get(name));
            }
        }
    }
    
    @Override
    public List<Concept> getLawConcepts(Law law) {
        return law.getConcepts();
    }

    @Override
    public List<NegativeLaw> getNegativeLaws() {
        List<NegativeLaw> result = new ArrayList<>();
        for (NegativeLaw law : super.getNegativeLaws()) {
            if (law.getName().equals("errors") ||
                law.getName().equals("operand_type") ||
                law.getName().equals("type_test_error") ||
                law.getName().equals("precedence_type")) {
                continue;
            }
            result.add(law);
        }
        return result;
    }

    @Override
    public List<Law> getQuestionLaws(String questionDomainType, List<Tag> tags) {
        return Collections.singletonList(new DTLaw(this.domainSolvingModel.getDecisionTree()));
    }

    // filter positive laws by question type and tags
    @Override
    public List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE) || questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE) || questionDomainType.equals(OPERANDS_TYPE_QUESTION_TYPE)
            || questionDomainType.equals(PRECEDENCE_TYPE_QUESTION_TYPE)) {
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
        return new ArrayList<>();
    }

    public List<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        return Collections.singletonList(new DTLaw(this.domainSolvingModel.getDecisionTree()));
    }

    //-----------ФАКТЫ---------------

    @Override
    public Collection<Fact> processQuestionFactsForBackendSolve(Collection<Fact> questionFacts) {
        its.model.definition.Domain situationModel = ProgrammingLanguageExpressionRDFTransformer.questionToDomainModel(
            domainSolvingModel.getDomain(),
            questionFacts.stream().map(Fact::asBackendFact).toList(),
            Collections.emptyList()
        );

        return Collections.singletonList(new DecisionTreeReasonerBackend.DomainFact(situationModel));
    }

    @Override
    public Collection<Fact> processQuestionFactsForBackendJudge(
            Collection<Fact> questionFacts,
            Collection<ResponseEntity> responses,
            Collection<Fact> responseFacts, Collection<Fact> solutionFacts) {
        its.model.definition.Domain situationModel = ProgrammingLanguageExpressionRDFTransformer.questionToDomainModel(
            domainSolvingModel.getDomain(),
            questionFacts.stream().map(Fact::asBackendFact).toList(),
            responses.stream().toList()
        );

        return Collections.singletonList(new DecisionTreeReasonerBackend.DomainFact(situationModel));
    }
    
    @Override
    public Question parseQuestionTemplate(InputStream stream) {
        return SerializableQuestion.deserialize(stream).toQuestion(this);
    }
    
    @Override
    @Deprecated
    public ExerciseForm getExerciseForm() {
        return null;
    }
    
    @Override
    @Deprecated
    public ExerciseEntity processExerciseForm(ExerciseForm ef) {
        return null;
    }
    
    
    private List<Question> readQuestions(InputStream inputStream) {
        List<Question> res = new ArrayList<>();
        Question[] questions = Arrays.stream(SerializableQuestion.deserializeMany(inputStream))
                .map(q -> q.toQuestion(this))
                .toArray(Question[]::new);
        Collections.addAll(res, questions);

        // write questionName to each template
        //TODO: make normal check, other question types also can be longer than questionName restriction
        for (Question q : res) {
            if (!q.isSupplementary()) {
                String qName = q.getQuestionText().getText();
                if (qName.length() > 254) qName = qName.substring(0, 254);
                q.getQuestionData().setQuestionName(qName);
            }
        }

        return res;
    }

    //--------Генерация вопросов--------
    
    static List<Question> QUESTIONS;
    @Override
    protected List<Question> getQuestionTemplates() {
        if (QUESTIONS == null) {
            QUESTIONS = readQuestions(this.getClass().getClassLoader().getResourceAsStream(QUESTIONS_CONFIG_PATH));
        }
        return QUESTIONS;
    }
    
    @Override
    public String getMessage(String base_question_text, Language preferred_language) {
        String key = base_question_text;
        if (!base_question_text.startsWith(MESSAGE_PREFIX)) {
            key = MESSAGE_PREFIX + base_question_text;
        }
        var found = localizationService.getMessage(key, Language.getLocale(preferred_language));
        if (found.equals(key))
            return base_question_text;
        return found;
    }
    
    private Question makeQuestionCopy(Question q, ExerciseAttemptEntity exerciseAttemptEntity, Language userLang) {
        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showTrace(true)
                .multipleSelectionEnabled(false)
                .orderNumberOptions(new OrderQuestionOptionsEntity.OrderNumberOptions(
                    "/",
                    OrderQuestionOptionsEntity.OrderNumberPosition.SUFFIX,
                    null
                ))
                .build();
        
        QuestionOptionsEntity matchingQuestionOptions = MatchingQuestionOptionsEntity.builder()
                .requireContext(false)
                .displayMode(MatchingQuestionOptionsEntity.DisplayMode.COMBOBOX)
                .build();
        
        QuestionOptionsEntity multiChoiceQuestionOptions = MultiChoiceOptionsEntity.builder()
                .requireContext(false)
                .build();
        
        QuestionOptionsEntity singleChoiceQuestionOptions = SingleChoiceOptionsEntity.builder()
                .requireContext(false)
                .build();
        
        QuestionEntity entity = new QuestionEntity();
        List<AnswerObjectEntity> answerObjectEntities = new ArrayList<>();
        for (AnswerObjectEntity answerObjectEntity : q.getAnswerObjects()) {
            AnswerObjectEntity newAnswerObjectEntity = new AnswerObjectEntity();
            newAnswerObjectEntity.setAnswerId(answerObjectEntity.getAnswerId());
            newAnswerObjectEntity.setConcept(answerObjectEntity.getConcept());
            newAnswerObjectEntity.setDomainInfo(answerObjectEntity.getDomainInfo());
            
            String text = getMessage(answerObjectEntity.getHyperText(), userLang);
            if (text.equals("")) {
                text = answerObjectEntity.getHyperText();
            }
            
            newAnswerObjectEntity.setHyperText(text);
            newAnswerObjectEntity.setQuestion(entity);
            newAnswerObjectEntity.setRightCol(answerObjectEntity.isRightCol());
            newAnswerObjectEntity.setResponsesLeft(new ArrayList<>());
            newAnswerObjectEntity.setResponsesRight(new ArrayList<>());
            answerObjectEntities.add(newAnswerObjectEntity);
        }
        entity.setAnswerObjects(answerObjectEntities);
        entity.setExerciseAttempt(exerciseAttemptEntity);
        entity.setQuestionDomainType(q.getQuestionDomainType());
        
        //TODO: remove this hack supporting old format
        if (!q.getStatementFacts().isEmpty() && (q.getStatementFacts().get(0).getVerb() == null)) {
            entity.setStatementFacts(getBackendFacts(q.getStatementFacts()));
        } else {
            entity.setStatementFacts(q.getStatementFacts());
            entity.setSolutionFacts(q.getStatementFacts());
        }
        entity.setQuestionType(q.getQuestionType());
        String qName = q.getQuestionName();
        if (qName == null) {
            qName = q.getQuestionType().toString(); // set anything
        }
        
        if (qName.length() > 254) qName = qName.substring(0, 254);
        entity.setQuestionName(qName);
        
        String text = q.getQuestionText().getText();
        if (text.startsWith(MESSAGE_PREFIX) || text.startsWith(SUPPLEMENTARY_PREFIX)) {
            text = getMessage(text, userLang);
        }
        
        switch (q.getQuestionType()) {
            case ORDER:
                val baseQuestionText = getMessage("BASE_QUESTION_TEXT", userLang);
                //TODO: remove this hack supporting old format
                if (!q.getStatementFacts().isEmpty() && (q.getStatementFacts().get(0).getVerb() == null)) {
                    entity.setQuestionText(baseQuestionText + expressionToHtml(q.getStatementFacts()));
                } else {
                    text = reformatQuestionText(q);
                    if (true) {
                        // DEBUG: add question name as html comment
                        var name = q.getQuestionName();
                        name = "<!-- question name: " + name + " -->";
                        text = name + text;
                    }
                    
                    entity.setQuestionText(baseQuestionText + text
                            .replace("end evaluation", getMessage("END_EVALUATION", userLang))
                            .replace("student_end_evaluation", getMessage("STUDENT_END_EVALUATION", userLang)));
                }
                entity.setOptions(orderQuestionOptions);
                Question question = new Question(entity, this);
                // patch the newly created question with the concepts from the "template"
                question.getConcepts().addAll(q.getConcepts());
                // ^ shouldn't this be done in a more straightforward way..?
                return question;
            case MATCHING:
                entity.setQuestionText(QuestionTextToHtml(text));
                entity.setOptions(matchingQuestionOptions);
                return new Question(entity, this);
            case MULTI_CHOICE:
                entity.setQuestionText(QuestionTextToHtml(text));
                entity.setOptions(multiChoiceQuestionOptions);
                return new Question(entity, this);
            case SINGLE_CHOICE:
                entity.setQuestionText(QuestionTextToHtml(text));
                entity.setOptions(singleChoiceQuestionOptions);
                return new Question(entity, this);
            default:
                throw new UnsupportedOperationException("Unknown type in ProgrammingLanguageExpressionDTDomain::makeQuestion: " + q.getQuestionType());
        }
    }
    
    @Override
    public QuestionRequest ensureQuestionRequestValid(QuestionRequest qr) {
        qr = super.ensureQuestionRequestValid(qr);
        
        // hard limits on solution length (questions outside this boundaries will never appear)
        qr.setStepsMin(2);
        qr.setStepsMax(23);
        
        return qr;
    }
    
    @NotNull
    private static String reformatQuestionText(Question q) {
        // avoid changing generated files: re-generate html
        OntModel m = factsToOntModel(q.getStatementFacts());
        OntProperty text_prop = m.createOntProperty("http://vstu.ru/poas/code#text");
        OntProperty token_type = m.createOntProperty("http://vstu.ru/poas/code#token_type");
        OntProperty index = m.createOntProperty("http://vstu.ru/poas/code#index");
        OntProperty not_selectable = m.createOntProperty("http://vstu.ru/poas/code#not_selectable");
        // get facts for expression tokens (ordered)
        List<BackendFactEntity> expression = new ArrayList<>();
        List<Statement> operandStatements = m.listStatements(null, text_prop, (String) null).toList();
        // sort array by subject's index (so tokens are in right order)
        operandStatements.sort(Comparator.comparing(a -> Optional.ofNullable(
                        a.getSubject().getProperty(index))
                .map(Statement::getInt)
                .orElse(1000000)));
        // statements to facts
        for (Statement st : operandStatements) {
            String tokenText = st.getString();
            String tokenType = "variable";
            Statement st_token = st.getSubject().getProperty(token_type);
            if (st_token != null) {
                Statement st_not_selectable = st.getSubject().getProperty(not_selectable);
                if (st_not_selectable == null)
                    tokenType = "operator";
                // else: "variable"
            } else {
                tokenType = END_EVALUATION;
                tokenText = END_EVALUATION;
            }
            
            BackendFactEntity fact = new BackendFactEntity(
                    tokenType,
                    "text", // probably redundant, may be null (?)
                    tokenText);
            expression.add(fact);
        }
        
        // replace with generated html
        String text = ExpressionToHtmlEnablingButtonDuplicates(expression);
        return text;
    }
    
    @Override
    public Question makeQuestion(ExerciseAttemptEntity exerciseAttempt, QuestionRequest questionRequest, List<Tag> tags, Language userLanguage) {
        
        HashSet<String> conceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getTargetConcepts()) {
            conceptNames.add(concept.getName());
        }
        
        List<Question> foundQuestions = null;
        if (!conceptNames.contains("SystemIntegrationTest")) {
            try {
                // new version - invoke rdfStorage search
                questionRequest = ensureQuestionRequestValid(questionRequest);
                foundQuestions = qMetaStorage.searchQuestions(this, exerciseAttempt, questionRequest, 1);
                
                // search again if nothing found with "TO_COMPLEX"
                SearchDirections lawsSearchDir = questionRequest.getLawsSearchDirection();
                if (foundQuestions.isEmpty() && lawsSearchDir == SearchDirections.TO_COMPLEX) {
                    questionRequest.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
                    foundQuestions = qMetaStorage.searchQuestions(this, exerciseAttempt, questionRequest, 1);
                }
            } catch (Exception e) {
                // file storage was not configured properly...
                log.error("Error searching questions - {}", e.getMessage(), e);
                foundQuestions = new ArrayList<>();
            }
        }
        
        Question res;
        if (foundQuestions != null && !foundQuestions.isEmpty()) {
            res = foundQuestions.get(0);
        } else {
            // old version - search in domain's in-memory questions
            // Prepare concept name sets ...
            HashSet<String> deniedConceptNames = new HashSet<>();
            for (Concept concept : questionRequest.getDeniedConcepts()) {
                deniedConceptNames.add(concept.getName());
            }
            deniedConceptNames.add("supplementary");
            
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
            if (exerciseAttempt != null &&
                    exerciseAttempt.getQuestions() != null &&
                    !exerciseAttempt.getQuestions().isEmpty()) {
                deniedQuestions.add(exerciseAttempt.getQuestions().get(exerciseAttempt.getQuestions().size() - 1).getQuestionName());
            }
            questionRequest.setDeniedQuestionNames(List.of());
            
            res = findQuestion(tags, conceptNames, deniedConceptNames, lawNames, deniedLawNames, deniedQuestions);
        }
        
        if (res == null) {
            throw new IllegalStateException("No valid questions found");
        }

        log.info("Expression domain has prepared the question: {}", res.getQuestionName());
        return makeQuestionCopy(res, exerciseAttempt, userLanguage);
    }
    
    public static String expressionToHtml(List<BackendFactEntity> expression) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p class='comp-ph-expr'>");
        int idx = 0;
        int answerIdx = -1;
        for (BackendFactEntity fact : expression) {
            String tokenValue = "";
            if (fact.getSubjectType() != null) { // Token has value
                tokenValue = fact.getSubjectType();
            }
            
            if (fact.getSubject() != null && fact.getSubject().equals("operator")) {
                sb.append("<span data-comp-ph-pos='").append(++idx).append("' id='answer_").append(++answerIdx).append("' class='comp-ph-expr-op-btn' data-comp-ph-value='").append(tokenValue).append("'>").append(fact.getObject()).append("</span>");
            } else if (fact.getSubject() != null && fact.getSubject().equals(END_EVALUATION)) {
                sb.append("<span data-comp-ph-pos='").append(++idx).append("' id='answer_").append(++answerIdx).append("' class='btn comp-ph-complete-btn' data-comp-ph-value=''>").append(fact.getObject()).append("</span>");
            } else {
                sb.append("<span data-comp-ph-pos='").append(++idx).append("' class='comp-ph-expr-const' data-comp-ph-value='").append(tokenValue).append("'>").append(fact.getObject()).append("</span>");
            }
        }
        
        sb.append("<!-- Original expression: ");
        for (BackendFactEntity fact : expression) {
            sb.append(fact.getObject()).append(" ");
        }
        sb.append("-->").append("</p>");
        return QuestionTextToHtml(sb.toString());
    }
    
    public static String ExpressionToHtmlEnablingButtonDuplicates(List<BackendFactEntity> expression) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p class='comp-ph-expr'>");
        int idx = 0;
        int answerIdx = -1;
        List<Integer> answerIdxStack = new ArrayList<>();  // pairedTwoTokenFirstAnswerIdxStack
        // todo: save placeholders or "empty" (non-operator) tokens as well
        for (int i = 0, expressionSize = expression.size(); i < expressionSize; i++) {
            BackendFactEntity fact = expression.get(i);
            String tokenValue = "";
            // suppress the value of token (always leave empty)
            /*if (fact.getSubjectType() != null) { // Token has value
                tokenValue = fact.getSubjectType();
            }*/
            
            if (fact.getSubject() != null && fact.getSubject().equals("operator")) {
                sb.append("<span data-comp-ph-pos='").append(++idx).append("' id='answer_").append(++answerIdx).append("' class='comp-ph-expr-op-btn' data-comp-ph-value='").append(tokenValue).append("'>").append(HtmlUtils.htmlEscape(fact.getObject())).append("</span>");
                // remember answer index of the first token of two-token operator
                if (List.of("(", "[", "?").contains(fact.getObject())) {
                    answerIdxStack.add(answerIdx);
                }
            } else if (fact.getSubject() != null && fact.getSubject().equals(END_EVALUATION)) {
                sb.append("<br/><button data-comp-ph-pos='").append(++idx).append("' id='answer_").append(++answerIdx).append("' class='btn comp-ph-complete-btn'>").append(/*fact.getObject()*/ END_EVALUATION).append("</button>");
            } else {
                boolean needAddOrdinaryToken = true;
                Integer answerIdxForButton = null;
                // remember answer index of the first token of two-token operator
                if (!answerIdxStack.isEmpty() && List.of(")", "]", ":").contains(fact.getObject())) {
                    // we need a clickable token with the same answer index as its first counterpart (see below)
                    answerIdxForButton = answerIdxStack.remove(answerIdxStack.size() - 1);
                    if (answerIdxForButton != null) {
                        needAddOrdinaryToken = false;
                    }
                }
                if (i < expressionSize - 1 - 1) {  // not last, + ")" must be later
                    // check out next token if it is a function's "("
                    BackendFactEntity nextFact = expression.get(i + 1);
                    if (nextFact.getObject().equals("(") && nextFact.getSubject().equals("operator")) {
                        answerIdxForButton = answerIdx + 1; // value of the following token
                        needAddOrdinaryToken = false;
                    }
                }
                if (!needAddOrdinaryToken) {
                    // add clickable token instead of plain token
                    sb.append("<span data-comp-ph-pos='").append(++idx).append("' id='answer_").append(answerIdxForButton).append("' class='comp-ph-expr-op-btn' data-comp-ph-value='").append(tokenValue).append("'>").append(HtmlUtils.htmlEscape(fact.getObject())).append("</span>");
                }
                if (needAddOrdinaryToken) {
                    // add ordinary token
                    sb.append("<span data-comp-ph-pos='").append(++idx).append("' class='comp-ph-expr-const' " +
                            "data-comp-ph-value='").append(tokenValue).append("'>").append(HtmlUtils.htmlEscape(fact.getObject())).append("</span>");
                    
                    // save placeholders or "empty" (non-operator) tokens as well: this keeps the stack valid
                    if (List.of("(", "[", "?").contains(fact.getObject())) {
                        answerIdxStack.add(null);
                    }
                }
            }
        }
        
        sb.append("<!-- Original expression: ");
        for (BackendFactEntity fact : expression) {
            sb.append(fact.getObject()).append(" ");
        }
        sb.append("-->").append("</p>");
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
    
    public List<BackendFactEntity> getBackendFacts(List<BackendFactEntity> expression) {
        List<BackendFactEntity> facts = new ArrayList<>();
        int index = 0;
        for (BackendFactEntity token : expression) {
            index++;
            String name = getName(0, index);
            facts.add(new BackendFactEntity(name, "rdf:type", "owl:NamedIndividual"));
            facts.add(new BackendFactEntity("owl:NamedIndividual", name, "index", "xsd:int", String.valueOf(index)));
            facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "text", "xsd:string", token.getObject()));
            if (token.getVerb() != null) {
                facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), token.getVerb(), token.getSubjectType(), token.getSubject()));
            }
            if (token.getObjectType() != null) { // Hack to insert boolean result
                facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "has_value", "xsd:boolean", token.getObjectType()));
            }
        }
        facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, 1), "first", "xsd:boolean", "true"));
        facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, expression.size()), "last_index", "xsd:boolean", "true"));
        return facts;
    }
    
    @Override
    public String getDefaultQuestionType(boolean supplementary) {
        return supplementary
                ? EVALUATION_ORDER_SUPPLEMENTARY_QUESTION_TYPE
                : EVALUATION_ORDER_QUESTION_TYPE;
    }
    
    @Override
    public List<Tag> getDefaultQuestionTags(String questionDomainType) {
        if (Objects.equals(questionDomainType, EVALUATION_ORDER_QUESTION_TYPE)) {
            return Stream.of("basics", "operators", "order", "evaluation", "errors", "C++")
                .map(this::getTag)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        return super.getDefaultQuestionTags(questionDomainType);
    }

    //-----Суждение вопросов и подобное ------
    
    public Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return new HashSet<>(); //Не нужно для DT
    }
    
    public Set<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return new HashSet<>(); //Не нужно для DT
    }
    
    @Override
    public Collection<Fact> responseToFacts(
        String questionDomainType,
        List<ResponseEntity> responses,
        List<AnswerObjectEntity> answerObjects
    ) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            List<Fact> result = new ArrayList<>();
            int pos = 1;
            HashSet<String> used = new HashSet<>();
            for (ResponseEntity response : responses) {
                result.add(new Fact(
                        "owl:NamedIndividual",
                        response.getLeftAnswerObject().getDomainInfo(),
                        "student_pos_number",
                        "xsd:int",
                        String.valueOf(pos)
                ));
                for (String earlier : used) {
                    result.add(new Fact(
                            "owl:NamedIndividual",
                            earlier,
                            "student_pos_less",
                            "owl:NamedIndividual",
                            response.getLeftAnswerObject().getDomainInfo()
                    ));
                }
                if (response.getLeftAnswerObject().getDomainInfo().equals("end_token")) {
                    result.add(new Fact(
                            "owl:NamedIndividual",
                            "end_token",
                            END_EVALUATION,
                            "xsd:boolean",
                            "true"
                    ));
                }
                used.add(response.getLeftAnswerObject().getDomainInfo());
                pos = pos + 1;
            }
            
            for (AnswerObjectEntity answerObject : answerObjects) {
                if (!used.contains(answerObject.getDomainInfo())) {
                    for (String earlier : used) {
                        result.add(new Fact(
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
            List<Fact> result = new ArrayList<>();
            for (ResponseEntity response : responses) {
                result.add(new Fact(
                        "owl:NamedIndividual",
                        response.getLeftAnswerObject().getDomainInfo(),
                        "student_type",
                        "xsd:string",
                        response.getRightAnswerObject().getDomainInfo()
                ));
            }
            return result;
        } else if (questionDomainType.equals(OPERANDS_TYPE_QUESTION_TYPE)) {
            List<Fact> result = new ArrayList<>();
            for (ResponseEntity response : responses) {
                result.add(new Fact(
                        "owl:NamedIndividual",
                        response.getLeftAnswerObject().getDomainInfo(),
                        "student_operand_type",
                        "xsd:string",
                        response.getRightAnswerObject().getDomainInfo()
                ));
            }
            return result;
        } else if (questionDomainType.equals(PRECEDENCE_TYPE_QUESTION_TYPE)) {
            List<Fact> result = new ArrayList<>();
            for (ResponseEntity response : responses) {
                result.add(new Fact(
                        "owl:NamedIndividual",
                        response.getLeftAnswerObject().getDomainInfo(),
                        "student_precedence_type",
                        "xsd:string",
                        response.getRightAnswerObject().getDomainInfo()
                ));
            }
            return result;
        }
        return new ArrayList<>();
    }
    
    @Override
    @Deprecated
    public ProcessSolutionResult processSolution(Collection<Fact> solution) {
        return null;
    }
    
    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        val lastCorrectInteraction = Optional.ofNullable(q.getQuestionData().getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0)
                .reduce((first, second) -> second);
        
        val solution = Fact.entitiesToFacts(q.getSolutionFacts());
        assert solution != null;
        //TODO
        return null;
    }
    
    @Override
    @Deprecated
    public Set<String> possibleViolations(Question q, List<ResponseEntity> completedSteps) {
        return null;
    }
    
    @Override
    @Deprecated
    public Set<Set<String>> possibleViolationsByStep(Question q, List<ResponseEntity> completedSteps) {
        return null;
    }

    //----------Вспомогательные вопросы------------
    
    @Override
    public boolean needSupplementaryQuestion(ViolationEntity violation) {
        return true;
    }

    private its.model.definition.Domain mainQuestionToModel(InteractionEntity lastMainQuestionInteraction) {
        return ProgrammingLanguageExpressionRDFTransformer.questionToDomainModel(
            dtSupplementaryQuestionHelper.domainModel.getDomain(),
            lastMainQuestionInteraction.getQuestion(),
            lastMainQuestionInteraction
        );
    }
    
    private final DecisionTreeSupQuestionHelper dtSupplementaryQuestionHelper = new DecisionTreeSupQuestionHelper(
            this,
            this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION),
            this::mainQuestionToModel
    );
    
    @Override
    public SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionEntity sourceQuestion, ViolationEntity violation, Language lang) {
        return dtSupplementaryQuestionHelper.makeSupplementaryQuestion(sourceQuestion, lang);
    }
    
    @Override
    public SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(Question question, SupplementaryStepEntity supplementaryStep, List<ResponseEntity> responses) {
        return dtSupplementaryQuestionHelper.judgeSupplementaryQuestion(supplementaryStep, responses);
    }

    //-----------Объяснения---------------
    
    @Override
    public InterpretSentenceResult interpretSentence(Collection<Fact> violations) {
        List<ViolationEntity> mistakes = DecisionTreeReasonerBackend.reasonerOutputFactsToViolations(
            violations.stream().toList()
        );
        
        InterpretSentenceResult result = new InterpretSentenceResult();
        result.violations = mistakes;
        result.correctlyAppliedLaws = new ArrayList<>();
        result.isAnswerCorrect = mistakes.isEmpty();

//        ProcessSolutionResult processResult = processSolution(violations);
        result.CountCorrectOptions = 10;
        result.IterationsLeft = 10; //TODO
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
    
    public static OntModel factsToOntModel(List<BackendFactEntity> backendFacts) {
        JenaBackend jback = new JenaBackend();
        jback.createOntology(NS_code.base());
        
        OntModel model = jback.getModel();
        
        jback.addBackendFacts(backendFacts);
        
        return model;
    }

    //--------Трассировка-----------

    @Override
    public List<HyperText> getFullSolutionTrace(Question question) {
        Language lang = question.getQuestionData().getExerciseAttempt().getUser().getPreferred_language();
        
        ArrayList<HyperText> result = new ArrayList<>();
        
        String qType = question.getQuestionData().getQuestionDomainType();
        if (qType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            FactsGraph qg = new FactsGraph(question.getQuestionData().getStatementFacts());
            
            for (ResponseEntity response : responsesForTrace(question.getQuestionData(), true)) {
                StringJoiner builder = new StringJoiner(" ");
                builder.add("<span>" + getMessage("OPERATOR", lang) + "</span>");
                // format a trace line ...
                AnswerObjectEntity answerObj = response.getLeftAnswerObject();
                String domainInfo = answerObj.getDomainInfo();
                if (domainInfo.equals("end_token")) {
                    continue;
                }
                builder.add("<span style='color: #700;text-decoration: underline;'>" +
                        qg.filterFacts(domainInfo, "text", null).stream().findFirst().get().getObject() +
                        "</span>");
                builder.add("<span>" + getMessage("AT_POS", lang) + "</span>");
                builder.add("<span style='color: #f00;font-weight: bold;'>" +
                        qg.filterFacts(domainInfo, "index", null).stream().findFirst().get().getObject() +
                        "</span>");
                builder.add("<span>" + getMessage("CALCULATED", lang) + "</span>");
                
                List<BackendFactEntity> value = qg.filterFacts(domainInfo, "has_value", null);
                if (!value.isEmpty()) {
                    builder.add("<span>" + getMessage("WITH_VALUE", lang) + "</span>");
                    builder.add("<span style='color: #f08;font-style: italic;font-weight: bold;'>" +
                            value.get(0).getObject() +
                            "</span>");
                }
                
                boolean responseIsWrong = !response.getInteraction().getViolations().isEmpty();
                var finalHtml = responseIsWrong
                        ? "<span style='background-color: #ff9;'>" + builder + "</span>"
                        : builder.toString();
                result.add(new HyperText(finalHtml));
            }
        } else {
            ///
            result.addAll(Arrays.asList(
                    new HyperText("debugging trace line #1 for unknown question Type" + question.getQuestionData().getQuestionDomainType()),
                    new HyperText("trace <b>line</b> #2"),
                    new HyperText("trace <i>line</i> #3")
            ));
        }
        return result;
    }
    
    private List<ResponseEntity> responsesForTrace(QuestionEntity q, boolean allowLastIncorrect) {
        
        List<ResponseEntity> responses = new ArrayList<>();
        List<InteractionEntity> interactions = q.getInteractions();
        
        if (interactions == null || interactions.isEmpty()) {
            return responses; // empty so far
            // early exit: no further checks for emptiness
        }
        
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

    //-------Битмаски-----
    //Зачем?
    
    private HashMap<String, Long> _getConceptsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(26);
        name2bit.put("operator", 0x1L);  	// (1)
        name2bit.put("operator_,", 0x2L);  	// (2)
        name2bit.put("operator_==", 0x4L);  	// (4)
        name2bit.put("operator_!", 0x8L);  	// (8)
        name2bit.put("operator_&&", 0x10L);  	// (16)
        name2bit.put("operator_<=", 0x20L);  	// (32)
        name2bit.put("precedence", 0x40L);  	// (64)
        name2bit.put("associativity", 0x80L);  	// (128)
        name2bit.put("operator_!=", 0x100L);  	// (256)
        name2bit.put("operator_>=", 0x200L);  	// (512)
        name2bit.put("operator_binary_-", 0x400L);  	// (1024)
        name2bit.put("operator_||", 0x800L);  	// (2048)
        name2bit.put("operator_&", 0x1000L);  	// (4096)
        name2bit.put("operator_=", 0x2000L);  	// (8192)
        name2bit.put("operator_binary_+", 0x4000L);  	// (16384)
        name2bit.put("operator_/", 0x8000L);  	// (32768)
        name2bit.put("operator_unary_*", 0x10000L);  	// (65536)
        name2bit.put("operator_binary_*", 0x20000L);  	// (131072)
        name2bit.put("operator_<<", 0x40000L);  	// (262144)
        name2bit.put("operator_unary_-", 0x80000L);  	// (524288)
        name2bit.put("operator_|", 0x100000L);  	// (1048576)
        name2bit.put("operator_^", 0x200000L);  	// (2097152)
        name2bit.put("operator_<", 0x400000L);  	// (4194304)
        name2bit.put("operator_>", 0x800000L);  	// (8388608)
        name2bit.put("operator_postfix_++", 0x1000000L);  	// (16777216)
        name2bit.put("operator_binary_&", 0x2000000L);  	// (33554432)
        name2bit.put("operator_%", 0x4000000L);  	// (67108864)
        name2bit.put("operator_postfix_--", 0x8000000L);  	// (134217728)
        name2bit.put("operator_>>", 0x10000000L);  	// (268435456)
        name2bit.put("operator_+=", 0x20000000L);  	// (536870912)
        name2bit.put("operator_|=", 0x40000000L);  	// (1073741824)
        name2bit.put("operator_~", 0x80000000L);  	// (2147483648)
        name2bit.put("operator_&=", 0x100000000L);  	// (4294967296)
        name2bit.put("operator_unary_+", 0x200000000L);  	// (8589934592)
        name2bit.put("operator_-=", 0x400000000L);  	// (17179869184)
        name2bit.put("operator_/=", 0x800000000L);  	// (34359738368)
        name2bit.put("operator_<<=", 0x1000000000L);  	// (68719476736)
        name2bit.put("operator_>>=", 0x2000000000L);  	// (137438953472)
        name2bit.put("operator_(", 0x4000000000L);  	// (274877906944)
        name2bit.put("operator_->", 0x8000000000L);  	// (549755813888)
        name2bit.put("function_call", 0x10000000000L);  	// (1099511627776)  -- not `operator_function_call` !
        name2bit.put("operator_function_call", 0x10000000000L);  	// (1099511627776)  -- for probable back compatibility
        name2bit.put("operator_.", 0x20000000000L);  	// (2199023255552)
        name2bit.put("operator_subscript", 0x40000000000L);  	// (4398046511104)
        name2bit.put("operator_prefix_++", 0x80000000000L);  	// (8796093022208)
        name2bit.put("operator_prefix_--", 0x100000000000L);  	// (17592186044416)
        return name2bit;
        // (developer tip: see sqlite2mysql)
    }
    private HashMap<String, Long> _getViolationsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(16);
        name2bit.put("error_base_higher_precedence_right", 0x1L);    // (1)
        name2bit.put("error_base_student_error_early_finish", 0x2L);    // (2)
        name2bit.put("error_base_student_error_in_complex", 0x4L);    // (4)
        name2bit.put("error_base_same_precedence_right_associativity_right", 0x8L);    // (8)
        name2bit.put("error_base_higher_precedence_left", 0x10L);    // (16)
        name2bit.put("error_base_student_error_strict_operands_order", 0x20L);    // (32)
        name2bit.put("error_base_same_precedence_left_associativity_left", 0x40L);    // (64)
        name2bit.put("error_base_student_error_unevaluated_operand", 0x80L);    // (128)    }
        name2bit.put("associativity", 0x100L);  	// (256)
        name2bit.put("error_base_unary_having_associativity_right", 0x200L);  	// (512)
        name2bit.put("precedence", 0x400L);  	// (1024)
        name2bit.put("error_base_binary_having_associativity_left", 0x800L);  	// (2048)
        name2bit.put("error_base_binary_having_associativity_right", 0x1000L);  // (4096)
        name2bit.put("error_base_unary_having_associativity_left", 0x2000L);  	// (8192)
        name2bit.put("error_base_enclosing_operators", 0x4000L);  	// (16384)
        return name2bit;
    }
    private HashMap<String, Long> _getLawsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(8);
        name2bit.put("single_token_binary_execution", 0x1L);  	// (1)
        name2bit.put("two_token_binary_execution", 0x2L);  	// (2)
        name2bit.put("single_token_unary_prefix_execution", 0x4L);  	// (4)
        name2bit.put("two_token_unary_execution", 0x8L);  	// (8)
        name2bit.put("single_token_unary_postfix_execution", 0x10L);  	// (16)
        return name2bit;
    }
}
