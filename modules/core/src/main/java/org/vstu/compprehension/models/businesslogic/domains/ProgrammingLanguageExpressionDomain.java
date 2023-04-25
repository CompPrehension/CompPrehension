package org.vstu.compprehension.models.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.text.StringSubstitutor;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph;
import org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.models.entities.QuestionOptions.*;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataBaseRepository;
import org.vstu.compprehension.models.repository.QuestionRequestLogRepository;
import org.vstu.compprehension.utils.ExpressionSituationPythonCaller;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import javax.inject.Singleton;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.random;

@Component @Log4j2
@Singleton
public class ProgrammingLanguageExpressionDomain extends Domain {
    static final String EVALUATION_ORDER_QUESTION_TYPE = "OrderOperators";
    static final String EVALUATION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderOperatorsSupplementary";
    static final String OPERANDS_TYPE_QUESTION_TYPE = "OperandsType";
    static final String PRECEDENCE_TYPE_QUESTION_TYPE = "PrecedenceType";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
    static final String RESOURCES_LOCATION = "org/vstu/compprehension/models/businesslogic/domains/";
    static final String LAWS_CONFIG_PATH = RESOURCES_LOCATION + "programming-language-expression-domain-laws.json";
    static final String QUESTIONS_CONFIG_PATH = RESOURCES_LOCATION + "programming-language-expression-domain-questions.json";
    static final String SUPPLEMENTARY_CONFIG_PATH = RESOURCES_LOCATION + "programming-language-expression-domain-supplementary-strategy.json";
    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "programming-language-expression-domain-messages";
    
    static final String MESSAGE_PREFIX = "expr_domain.";
    static final String SUPPLEMENTARY_PREFIX = "supplementary.";

    public static final String VOCAB_SCHEMA_PATH = RESOURCES_LOCATION + "programming-language-expression-domain-schema.rdf";

    public static final String END_EVALUATION = "student_end_evaluation";
    private final LocalizationService localizationService;
    private final QuestionMetadataBaseRepository exprQuestionMetadataRepository;

    @Autowired
    public ProgrammingLanguageExpressionDomain(LocalizationService localizationService,
                                               DomainRepository domainRepository,
                                               RandomProvider randomProvider,
                                               QuestionMetadataBaseRepository exprQuestionMetadataRepository,
         QuestionRequestLogRepository questionRequestLogRepository) {
        super(randomProvider, questionRequestLogRepository);
        this.localizationService = localizationService;
        this.exprQuestionMetadataRepository = exprQuestionMetadataRepository;

        name = "ProgrammingLanguageExpressionDomain";
        domainEntity = domainRepository.findById(getDomainId()).orElseThrow();

        fillTags();
        fillConcepts();
        readLaws(this.getClass().getClassLoader().getResourceAsStream(LAWS_CONFIG_PATH));
        readSupplementaryConfig(this.getClass().getClassLoader().getResourceAsStream(SUPPLEMENTARY_CONFIG_PATH));
    }

    private ProgrammingLanguageExpressionDomain(LocalizationService localizationService) {
        super(new RandomProvider(), null);
        this.localizationService = localizationService;
        exprQuestionMetadataRepository = null;

        name = "ProgrammingLanguageExpressionDomain";
        // domainEntity = null;
        domainEntity = new DomainEntity();
        domainEntity.setOptions(new DomainOptionsEntity());

        fillTags();
        fillConcepts();
        readLaws(this.getClass().getClassLoader().getResourceAsStream(LAWS_CONFIG_PATH));
        readSupplementaryConfig(this.getClass().getClassLoader().getResourceAsStream(SUPPLEMENTARY_CONFIG_PATH));
        // using update() as init
        // OFF: // update();
    }
    //Hacked version. don't use in production, only for develop
    public static ProgrammingLanguageExpressionDomain makeHackedDomain() {
        return new ProgrammingLanguageExpressionDomain(new LocalizationService());
    }

    @Override
    public String getShortName() {
        return "expression";
    }

    @NotNull
    @Override
    public String getDomainId() {
        return "ProgrammingLanguageExpressionDomain";
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

    public static class SupplementaryAnswerTransition {
        public String check;
        public String question;
        public String detailed_law;
        public boolean correct;
    }

    public static class SupplementaryAnswerConfig {
        public String name;
        public List<SupplementaryAnswerTransition> transitions;
    }

    static class SupplementaryConfig {
        String name;
        List<SupplementaryAnswerConfig> answers;
    }

    private HashMap<String, HashMap<String, List<SupplementaryAnswerTransition>>> supplementaryConfig;
    public  HashMap<String, HashMap<String, List<SupplementaryAnswerTransition>>> getSupplementaryConfig() {
        return supplementaryConfig;
    }
    private void readSupplementaryConfig(InputStream inputStream) {
        supplementaryConfig = new HashMap<>();

        SupplementaryConfig[] configs = new Gson().fromJson(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                SupplementaryConfig[].class);

        for (SupplementaryConfig config : configs) {
            supplementaryConfig.put(config.name, new HashMap<>());
            for (SupplementaryAnswerConfig answer : config.answers) {
                supplementaryConfig.get(config.name).put(answer.name, answer.transitions);
            }
        }
    }

    private void readLaws(InputStream inputStream) {
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
    public void update() {
        // init questions storage
        getRdfStorage();
    }

    @Override
    public QuestionMetadataBaseRepository getQuestionMetadataRepository() {
        return exprQuestionMetadataRepository;
    }


    @Override
    public Question parseQuestionTemplate(InputStream stream) {
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
                new InputStreamReader(stream, StandardCharsets.UTF_8),
                Question.class);

        return question;
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
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                Question[].class);

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

    Question makeQuestionCopy(Question q, ExerciseAttemptEntity exerciseAttemptEntity, Language userLang) {
        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showTrace(true)
                .multipleSelectionEnabled(false)
                .orderNumberOptions(new OrderQuestionOptionsEntity.OrderNumberOptions("/", OrderQuestionOptionsEntity.OrderNumberPosition.SUFFIX, null))
                .templateId(q.getQuestionData().getOptions().getTemplateId())  // copy from loaded question
                .questionMetaId(q.getQuestionData().getOptions().getQuestionMetaId())
                .metadata(q.getQuestionData().getOptions().getMetadata())  // copy from loaded question
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
                    entity.setQuestionText(baseQuestionText + ExpressionToHtml(q.getStatementFacts()));
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
                Question question = new Ordering(entity, this);
                // patch the newly created question with the concepts from the "template"
                question.getConcepts().addAll(q.getConcepts());
                // ^ shouldn't this be done in a more straightforward way..?
                return question;
            case MATCHING:
                entity.setQuestionText(QuestionTextToHtml(text));
                entity.setOptions(matchingQuestionOptions);
                return new Matching(entity, this);
            case MULTI_CHOICE:
                entity.setQuestionText(QuestionTextToHtml(text));
                entity.setOptions(multiChoiceQuestionOptions);
                return new MultiChoice(entity, this);
            case SINGLE_CHOICE:
                entity.setQuestionText(QuestionTextToHtml(text));
                entity.setOptions(singleChoiceQuestionOptions);
                return new SingleChoice(entity, this);
            default:
                throw new UnsupportedOperationException("Unknown type in ProgrammingLanguageExpressionDomain::makeQuestion: " + q.getQuestionType());
        }
    }

    @Override
    public QuestionRequest fillBitmasksInQuestionRequest(QuestionRequest qr) {
        qr = super.fillBitmasksInQuestionRequest(qr);

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
    public Question makeQuestion(QuestionRequest questionRequest, List<Tag> tags, Language userLanguage) {

        HashSet<String> conceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getTargetConcepts()) {
            conceptNames.add(concept.getName());
        }

        List<Question> foundQuestions = null;
        if (!conceptNames.contains("SystemIntegrationTest")) {
            try {
                // new version - invoke rdfStorage search
                questionRequest = fillBitmasksInQuestionRequest(questionRequest);
                saveQuestionRequest(questionRequest);
                foundQuestions = getRdfStorage().searchQuestions(questionRequest, 1);

                // search again if nothing found with "TO_COMPLEX"
                SearchDirections lawsSearchDir = questionRequest.getLawsSearchDirection();
                if (foundQuestions.isEmpty() && lawsSearchDir == SearchDirections.TO_COMPLEX) {
                    questionRequest.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
                    foundQuestions = getRdfStorage().searchQuestions(questionRequest, 1);
                }
            } catch (RuntimeException ex) {
                // file storage was not configured properly...
                ex.printStackTrace();
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
            if (questionRequest.getExerciseAttempt() != null &&
                    questionRequest.getExerciseAttempt().getQuestions() != null &&
                    questionRequest.getExerciseAttempt().getQuestions().size() > 0) {
                deniedQuestions.add(questionRequest.getExerciseAttempt().getQuestions().get(questionRequest.getExerciseAttempt().getQuestions().size() - 1).getQuestionName());
            }
            questionRequest.setDeniedQuestionNames(List.of());

            res = findQuestion(tags, conceptNames, deniedConceptNames, lawNames, deniedLawNames, deniedQuestions);
        }

        if (res != null) {
            log.info("Expression domain has prepared the question: " + res.getQuestionName());

            return makeQuestionCopy(res, questionRequest.getExerciseAttempt(), userLanguage);
        }

        // make a SingleChoice question ...
        QuestionEntity question = new QuestionEntity();
        question.setExerciseAttempt(questionRequest.getExerciseAttempt());
        question.setQuestionText("Choose associativity of operator binary +");
        question.setQuestionType(QuestionType.SINGLE_CHOICE);
        question.setQuestionDomainType("ChooseAssociativity");
        question.setAnswerObjects(new ArrayList<>(Arrays.asList(
                createAnswerObject(question, 0, "left", "left_associativity", "left", true),
                createAnswerObject(question, 1, "right", "right_associativity", "right", true),
                createAnswerObject(question, 2, "no associativity", "absent_associativity", "no associativity", true)
        )));
        return new SingleChoice(question, this);
    }

    private AnswerObjectEntity createAnswerObject(QuestionEntity question, int id, String text, String concept, String domainInfo, boolean isLeft) {
        AnswerObjectEntity answerObject = new AnswerObjectEntity();
        answerObject.setAnswerId(id);
        answerObject.setHyperText(text);
        answerObject.setRightCol(!isLeft);
        answerObject.setDomainInfo(domainInfo);
        answerObject.setConcept(concept);
        answerObject.setQuestion(question);
        return answerObject;
    }

    public static String ExpressionToHtml(List<BackendFactEntity> expression) {
        StringBuilder sb = new StringBuilder("");
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
        StringBuilder sb = new StringBuilder("");
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

    /**
     * Get all needed (positive and negative) laws for this questionType using default tags
     * @param questionDomainType type of question
     * @return list of laws
     */
    public List<Law> getQuestionLaws(String questionDomainType /*, List<Tag> tags*/) {

        return getQuestionLaws(questionDomainType, getDefaultQuestionTags(questionDomainType));
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
    public Model getSchemaForSolving() {
        Model schemaModel = ModelFactory.createDefaultModel();
        // todo: cache it?
        return schemaModel.read(VOCAB_SCHEMA_PATH);
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
            return Stream.of("basics", "operators", "order", "evaluation", "errors", "C++").map(this::getTag).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return super.getDefaultQuestionTags(questionDomainType);
    }

//    @Override
//    public QuestionMetadataBaseRepository getQuestionMetadataRepository() {
//        return null;
//    }

    public List<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            List<NegativeLaw> negativeLaws = new ArrayList<>();
            for (NegativeLaw law : super.getNegativeLaws()) {
                boolean needLaw = true;
                //filter by tags after separation
                if (needLaw) {
                    negativeLaws.add(law);
                }
            }
            return negativeLaws;
        } else if (questionDomainType.equals(OPERANDS_TYPE_QUESTION_TYPE)) {
            List<NegativeLaw> negativeLaws = new ArrayList<>();
            for (NegativeLaw law : super.getNegativeLaws()) {
                boolean needLaw = true;
                if (needLaw) {
                    negativeLaws.add(law);
                }
            }
            return negativeLaws;
        }
        else if (questionDomainType.equals(PRECEDENCE_TYPE_QUESTION_TYPE)) {
            List<NegativeLaw> negativeLaws = new ArrayList<>();
            for (NegativeLaw law : super.getNegativeLaws()) {
                boolean needLaw = law.getName().equals("precedence_basics") || law.getName().equals("precedence_type");
                if (needLaw) {
                    negativeLaws.add(law);
                }
            }
            return negativeLaws;
        }
        return new ArrayList<>(List.of());
    }

    public Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE) || questionDomainType.equals(OPERANDS_TYPE_QUESTION_TYPE)) {
            return new HashSet<>(Arrays.asList(
                    "has_operand",
                    "has_left_operand",
                    "has_right_operand",
                    "has_inner_operand",
                    "before",
                    "before_direct",
                    "before_by_third_operator",
                    "before_third_operator",
                    "before_as_operand",
                    "is_operator_with_strict_operands_order",
                    "high_precedence",
                    "high_precedence_diff_precedence",
                    "high_precedence_left_assoc",
                    "high_precedence_right_assoc",
                    "is_operand",
                    "law_name",
                    "text",
                    "index",
                    "precedence",
                    "associativity",
                    "in_complex",
                    "complex_beginning",
                    "is_function_call",
                    "student_error_in_complex_base",
                    "student_error_more_precedence_base",
                    "student_error_right_assoc_base",
                    "student_error_strict_operands_order_base",
                    "student_error_unevaluated_operand_base",
                    "student_error_left_assoc_base",
                    "student_error_early_finish_base",
                    "has_value",
                    "has_uneval_operand",
                    "has_value_eval_restriction",
                    "not_selectable",
                    "student_end_evaluation"
            ));
        } else if (questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
            return new HashSet<>(List.of(
                    "get_type"
            ));
        } else if (questionDomainType.equals(PRECEDENCE_TYPE_QUESTION_TYPE)) {
            return new HashSet<>(List.of(
                    "high_precedence_diff_precedence"
            ));
        }
        return new HashSet<>();
    }

    public Set<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            return new HashSet<>(Arrays.asList(
                    "student_error_more_precedence",
                    "student_error_left_assoc",
                    "student_error_right_assoc",
                    "student_error_in_complex",
                    "student_error_strict_operands_order",
                    "student_error_unevaluated_operand",
                    "student_error_more_precedence_base",
                    "student_error_left_assoc_base",
                    "student_error_right_assoc_base",
                    "student_error_in_complex_base",
                    "student_error_strict_operands_order_base",
                    "student_error_unevaluated_operand_base",
                    "student_error_early_finish_base",
                    "student_error_early_finish",
                    "before_third_operator",
                    "text",
                    "index",
                    "before_direct",
                    "student_pos_number",
                    "is_operand",
                    "is_function_call",
                    "has_value",
                    "has_uneval_operand",
                    "has_value_eval_restriction",
                    "not_selectable",
                    "law_name",
                    "student_end_evaluation",
                    "has_operand"
            ));
        } else if (questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
            return new HashSet<>(List.of(
                    "wrong_type"
            ));
        } else if (questionDomainType.equals(OPERANDS_TYPE_QUESTION_TYPE)) {
            return new HashSet<>(Arrays.asList(
                    "has_operand",
                    "has_operand_part",
                    "student_operand_type",
                    "in_complex",
                    "target_operator",
                    "complex_beginning",
                    "index"
            ));
        } else if (questionDomainType.equals(PRECEDENCE_TYPE_QUESTION_TYPE)) {
        return new HashSet<>(Arrays.asList(
                "high_precedence_diff_precedence",
                "text",
                "index",
                "target_operator",
                "student_precedence_type"
        ));
    }
        return new HashSet<>();
    }

    @Override
    public Collection<Fact> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
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

    private static Optional<Integer> getIndexFromName(String name, boolean allowNotZeroStep) {
        assert name.startsWith("op__");
        String[] parts = name.split("__");
        assert 3 == parts.length;
        if (allowNotZeroStep || parts[1].equals("0")) {
            return Optional.of(Integer.parseInt(parts[2]));
        }
        return Optional.empty();
    }

    class CorrectAnswerImpl {
        String domainID;
        String lawName;
    }

    private List<CorrectAnswerImpl> getCorrectAnswers(Collection<Fact> solution) {
        Map<String, List<String>> before = new HashMap<>();
        Map<String, String> studentPos = new HashMap<>();
        Map<String, String> unevalOp = new HashMap<>();
        Map<String, String> operatorLawName = new HashMap<>();
        HashSet<String> isOperand = new HashSet<>();
        HashSet<String> allTokens = new HashSet<>();

        List<CorrectAnswerImpl> result = new ArrayList<>();
        for (Fact fact : solution) {
            if (fact.getVerb().equals("before_direct")) {
                if (!before.containsKey(fact.getObject())) {
                    before.put(fact.getObject(), new ArrayList<>());
                }
                before.get(fact.getObject()).add(fact.getSubject());
                allTokens.add(fact.getObject());
                allTokens.add(fact.getSubject());
            } else if (fact.getVerb().equals("student_pos_number")) {
                studentPos.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("has_uneval_operand")) {
                unevalOp.put(fact.getObject(), "true");
            } else if (fact.getVerb().equals("not_selectable")) {
                isOperand.add(fact.getSubject());
            } else if (fact.getVerb().equals("law_name")) {
                operatorLawName.put(fact.getSubject(), fact.getObject());
            }
        }

        for (String operator : allTokens) {
            if (!operator.startsWith("op__0") || isOperand.contains(operator) || unevalOp.containsKey(operator)) {
                continue;
            }
            boolean can = !studentPos.containsKey(operator);
            if (before.containsKey(operator)) {
                List<String> deps = before.get(operator);
                for (String dep : deps) {
                    if (!studentPos.containsKey(dep) && !(isOperand.contains(dep) || unevalOp.containsKey(dep))) {
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

        if (result.isEmpty()) {
            CorrectAnswerImpl answer = new CorrectAnswerImpl();
            answer.domainID = "end_token";
            answer.lawName = "end_token";
            result.add(answer);
        }

        return result;
    }

    @Override
    public ProcessSolutionResult processSolution(Collection<Fact> solution) {
        Map<String, String> studentPos = new HashMap<>();
        Map<String, String> unevalOp = new HashMap<>();
        HashSet<String> isOperand = new HashSet<>();
        HashSet<String> isNotCallableOperator = new HashSet<>();
        HashSet<String> allTokens = new HashSet<>();
        boolean hasEndToken = false;
        for (Fact fact : solution) {
            if (fact.getVerb().equals("before_direct")) {
                allTokens.add(fact.getObject());
                allTokens.add(fact.getSubject());
            } else if (fact.getVerb().equals("student_pos_number")) {
                studentPos.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("has_uneval_operand")) {
                unevalOp.put(fact.getObject(), "true");
            } else if (fact.getVerb().equals("is_operand")) {
                isOperand.add(fact.getSubject());
            } else if (fact.getVerb().equals("is_function_call") && fact.getObject().equals("false")) {
                isNotCallableOperator.add(fact.getSubject());
            } else if (fact.getSubject().equals("end_token")) {
                hasEndToken = true;
            }
        }

        int IterationsLeft = 0;
        for (String operator : allTokens) {
            if (operator.startsWith("op__0") &&
                    !isOperand.contains(operator) &&
                    !isNotCallableOperator.contains(operator) &&
                    !studentPos.containsKey(operator) &&
                    !unevalOp.containsKey(operator)) {
                IterationsLeft++;
            }
            if (operator.startsWith("op__0") &&
                    studentPos.containsKey(operator) &&
                    unevalOp.containsKey(operator)) {
                IterationsLeft--; //ignore that mistake
            }
        }
        if (hasEndToken && !studentPos.containsKey("end_token")) {
            IterationsLeft++;
        }

        InterpretSentenceResult result = new InterpretSentenceResult();
        result.CountCorrectOptions = getCorrectAnswers(solution).size();
        result.IterationsLeft = IterationsLeft;
        return result;
    }

    HyperText getCorrectExplanation(Question q, AnswerObjectEntity answer) {
        HashMap<String, String> indexes = new HashMap<>();
        HashMap<String, String> texts = new HashMap<>();
        HashMap<String, String> isStrict = new HashMap<>();
        MultiValuedMap<String, String> before = new HashSetValuedHashMap<>();
        MultiValuedMap<String, String> beforeIndirectReversed = new HashSetValuedHashMap<>();
        MultiValuedMap<String, String> beforeHighPriority = new HashSetValuedHashMap<>();
        MultiValuedMap<String, String> beforeLeftAssoc = new HashSetValuedHashMap<>();
        MultiValuedMap<String, String> beforeRightAssoc = new HashSetValuedHashMap<>();
        MultiValuedMap<String, String> beforeByThirdOperator = new HashSetValuedHashMap<>();
        MultiValuedMap<String, String> beforeThirdOperator = new HashSetValuedHashMap<>();
        MultiValuedMap<String, String> beforeAsOperand = new HashSetValuedHashMap<>();

        for (BackendFactEntity fact : q.getSolutionFacts()) {
            if (fact.getVerb().equals("before_direct")) {
                before.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("before")) {
                beforeIndirectReversed.put(fact.getObject(), fact.getSubject());
            } else if (fact.getVerb().equals("before_by_third_operator")) {
                beforeByThirdOperator.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("before_third_operator")) {
                beforeThirdOperator.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("high_precedence_diff_precedence")) {
                beforeHighPriority.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("high_precedence_left_assoc")) {
                beforeLeftAssoc.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("high_precedence_right_assoc")) {
                beforeRightAssoc.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("before_as_operand")) {
                beforeAsOperand.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("index")) {
                indexes.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("text")) {
                texts.put(fact.getSubject(), fact.getObject());
            }  else if (fact.getVerb().equals("is_operator_with_strict_operands_order")) {
                isStrict.put(fact.getSubject(), fact.getObject());
            }
        }

        if (answer.getDomainInfo().equals("end_token")) {
            //TODO: make explanation why expression is ended
            return new HyperText("");
        }

        AnswerObjectEntity last = null;
        ArrayList<AnswerObjectEntity> explain = new ArrayList<>();
        TreeMap<Integer, String> posToExplanation = new TreeMap<>();
        
        Language lang = q.getQuestionData().getExerciseAttempt().getUser().getPreferred_language();

        int answerPos = Integer.parseInt(indexes.get(answer.getDomainInfo()));
        String answerText = texts.get(answer.getDomainInfo());
        String answerTemplate = StringHelper.joinWithSpace(answerText, getMessage("AT_POS", lang), answerPos);
        posToExplanation.put(-1, StringHelper.joinWithSpace(getMessage("OPERATOR", lang), answerTemplate, getMessage("EVALUATES", lang)));

        for (AnswerObjectEntity answerObjectEntity : q.getAnswerObjects()) {
            if (beforeByThirdOperator.containsKey(answerObjectEntity.getDomainInfo())) {
                for (String leftPart : beforeIndirectReversed.get(answerObjectEntity.getDomainInfo())) {
                    for (String rightPart : beforeByThirdOperator.get(answerObjectEntity.getDomainInfo())) {
                        beforeByThirdOperator.put(leftPart, rightPart);
                    }
                    for (String rightPart : beforeThirdOperator.get(answerObjectEntity.getDomainInfo())) {
                        beforeThirdOperator.put(leftPart, rightPart);
                    }
                }
            }
        }

        for (AnswerObjectEntity answerObjectEntity : q.getAnswerObjects()) {
            if (answer == answerObjectEntity && last != null) {
                explain.add(last);
            } else if (answer == last) {
                explain.add(answerObjectEntity);
            } else if (beforeByThirdOperator.containsMapping(answer.getDomainInfo(), answerObjectEntity.getDomainInfo())) {
                for (String thirdOperator : beforeThirdOperator.get(answer.getDomainInfo())) {
                    // explain highest in right half of strict
                    if (before.containsMapping(answerObjectEntity.getDomainInfo(), thirdOperator)) {
                        int pos = Integer.parseInt(indexes.get(answerObjectEntity.getDomainInfo()));
                        String text = texts.get(answerObjectEntity.getDomainInfo());
                        String template = StringHelper.joinWithSpace(text, getMessage("AT_POS", lang), pos);

                        int thirdPos = Integer.parseInt(indexes.get(thirdOperator));
                        String thirdText = texts.get(thirdOperator);
                        String thirdTemplate = StringHelper.joinWithSpace(thirdText, getMessage("AT_POS", lang), thirdPos);

                        if (isStrict.containsKey(thirdOperator)) {
                            posToExplanation.put(pos, StringHelper.joinWithSpace(
                                    getMessage("BEFORE_OPERATOR", lang),
                                    template,
                                    ":",
                                    getMessage("OPERATOR", lang),
                                    answerTemplate,
                                    getMessage("LEFT_SUBOPERATOR", lang),
                                    thirdTemplate,
                                    getMessage("WHILE_OPERATOR", lang),
                                    template,
                                    getMessage("TO_LEFT_OPERAND", lang) + ",",
                                    getMessage("AND_LEFT_OPERAND", lang),
                                    thirdText,
                                    getMessage("EVALUATES_BEFORE_RIGHT", lang)));
                        }
                    }
                }
            } else if (beforeByThirdOperator.containsMapping(answerObjectEntity.getDomainInfo(), answer.getDomainInfo())) {
                for (String thirdOperator : beforeThirdOperator.get(answerObjectEntity.getDomainInfo())) {
                    // explain highest in left half of strict
                    if (before.containsMapping(answerObjectEntity.getDomainInfo(), thirdOperator)) {
                        int pos = Integer.parseInt(indexes.get(answerObjectEntity.getDomainInfo()));
                        String text = texts.get(answerObjectEntity.getDomainInfo());
                        String template = StringHelper.joinWithSpace(text, getMessage("AT_POS", lang), pos);

                        int thirdPos = Integer.parseInt(indexes.get(thirdOperator));
                        String thirdText = texts.get(thirdOperator);
                        String thirdTemplate = StringHelper.joinWithSpace(thirdText, getMessage("AT_POS", lang), thirdPos);

                        if (isStrict.containsKey(thirdOperator)) {
                            posToExplanation.put(pos, StringHelper.joinWithSpace(
                                    getMessage("AFTER_OPERATOR", lang),
                                    template,
                                    ":",
                                    getMessage("OPERATOR", lang),
                                    answerTemplate,
                                    getMessage("RIGHT_SUBOPERATOR", lang),
                                    thirdTemplate,
                                    getMessage("WHILE_OPERATOR", lang),
                                    template,
                                    getMessage("TO_LEFT_OPERAND", lang) + ",",
                                    getMessage("AND_LEFT_OPERAND", lang),
                                    thirdText,
                                    getMessage("EVALUATES_BEFORE_RIGHT", lang)));
                        } else if (thirdText.equals("(")) {
                            posToExplanation.put(pos, StringHelper.joinWithSpace(
                                    getMessage("AFTER_OPERATOR", lang),
                                    template,
                                    ":",
                                    getMessage("OPERATOR", lang),
                                    template,
                                    getMessage("ENCLOSED_PARENTHESIS", lang),
                                    thirdPos,
                                    getMessage("INSIDE_PARENTHESIS_FIRST", lang)));
                        }
                    }
                }
            }
            last = answerObjectEntity;
        }

        for (AnswerObjectEntity reason : explain) {
            if (reason.getDomainInfo().equals("end_token")) {
                continue;
            }
            int pos = Integer.parseInt(indexes.get(reason.getDomainInfo()));
            String text = texts.get(reason.getDomainInfo());
            String template = StringHelper.joinWithSpace(text, getMessage("AT_POS", lang), pos);

            if (beforeHighPriority.containsMapping(answer.getDomainInfo(), reason.getDomainInfo())) {
                posToExplanation.put(pos, StringHelper.joinWithSpace(
                        getMessage("BEFORE_OPERATOR", lang), template, ":", getMessage("OPERATOR", lang),
                        answerText, getMessage("HAS_HIGHER_PRECEDENCE", lang), getMessage("THAN_OPERATOR", lang), text));
            } else if (beforeHighPriority.containsMapping(reason.getDomainInfo(), answer.getDomainInfo())) {
                posToExplanation.put(pos, StringHelper.joinWithSpace(
                        getMessage("AFTER_OPERATOR", lang), template, ":", getMessage("OPERATOR", lang),
                        answerText, getMessage("HAS_LOWER_PRECEDENCE", lang), getMessage("THAN_OPERATOR", lang), text));
            } else if (beforeLeftAssoc.containsMapping(answer.getDomainInfo(), reason.getDomainInfo())) {
                posToExplanation.put(pos, StringHelper.joinWithSpace(
                        getMessage("BEFORE_OPERATOR", lang), template, ":", getMessage("OPERATOR", lang),
                        answerText, getMessage("LEFT_ASSOC_DESC", lang)));
            } else if (beforeLeftAssoc.containsMapping(reason.getDomainInfo(), answer.getDomainInfo())) {
                posToExplanation.put(pos, StringHelper.joinWithSpace(
                        getMessage("AFTER_OPERATOR", lang), template, ":", getMessage("OPERATOR", lang),
                        answerText, getMessage("LEFT_ASSOC_DESC", lang)));
            } else if (beforeRightAssoc.containsMapping(answer.getDomainInfo(), reason.getDomainInfo())) {
                posToExplanation.put(pos, StringHelper.joinWithSpace(
                        getMessage("BEFORE_OPERATOR", lang), template, ":", getMessage("OPERATOR", lang),
                        answerText, getMessage("RIGHT_ASSOC_DESC", lang)));
            } else if (beforeRightAssoc.containsMapping(reason.getDomainInfo(), answer.getDomainInfo())) {
                posToExplanation.put(pos, StringHelper.joinWithSpace(
                        getMessage("AFTER_OPERATOR", lang), template, ":", getMessage("OPERATOR", lang),
                        answerText, getMessage("RIGHT_ASSOC_DESC", lang)));
            }
        }

        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, String> kv : posToExplanation.entrySet()) {
            result.append(kv.getValue()).append("\n");
        }

        return new HyperText(result.toString());
    }

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        val lastCorrectInteraction = Optional.ofNullable(q.getQuestionData().getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0)
                .reduce((first, second) -> second);
        /*val lastCorrectInteractionAnswers = lastCorrectInteraction
                .flatMap(i -> Optional.ofNullable(i.getResponses())).stream()
                .flatMap(Collection::stream)
                .map(r -> new CorrectAnswer.Response(r.getLeftAnswerObject(), r.getRightAnswerObject(), r.getCreatedByInteraction().getInteractionType() == InteractionType.SEND_RESPONSE))
                .collect(Collectors.toList());*/

        val solution = Fact.entitiesToFacts(q.getSolutionFacts());
        assert solution != null;
        solution.addAll(lastCorrectInteraction
                .flatMap(i -> Optional.ofNullable(responseToFacts(q.getQuestionDomainType(), i.getResponses(), q.getAnswerObjects()))).stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));

        List<CorrectAnswerImpl> correctAnswerImpls = getCorrectAnswers(solution);
        for (AnswerObjectEntity answer : q.getAnswerObjects()) {
            for (CorrectAnswerImpl answerImpl : correctAnswerImpls) {
                if (answerImpl.domainID.equals(answer.getDomainInfo())) {
                    val answers = new ArrayList<CorrectAnswer.Response>();
                    answers.add(new CorrectAnswer.Response(answer, answer));

                    CorrectAnswer correctAnswer = new CorrectAnswer();
                    correctAnswer.question = q.getQuestionData();
                    correctAnswer.answers = answers;
                    correctAnswer.lawName = answerImpl.lawName;
                    correctAnswer.explanation = getCorrectExplanation(q, answer);
                    return correctAnswer;
                }
            }
        }
        return null;
    }

    @Override
    public Set<String> possibleViolations(Question q, List<ResponseEntity> completedSteps) {
        return possibleViolations(q.getSolutionFacts(), completedSteps);
    }

    public Set<String> possibleViolations(List<BackendFactEntity> solutionFacts, List<ResponseEntity> completedSteps) {
        Set<String> result = new HashSet<>();
        Set<String> madeSteps = new HashSet<>();
        if (completedSteps != null) {
            for (ResponseEntity r : completedSteps) {
                madeSteps.add(r.getLeftAnswerObject().getDomainInfo());
                madeSteps.add(r.getRightAnswerObject().getDomainInfo());
            }
        }
        for (BackendFactEntity f : solutionFacts) {
            if (f.getVerb().startsWith("student_error") && !madeSteps.contains(f.getSubject()) && !madeSteps.contains(f.getObject())
                    && getIndexFromName(f.getSubject(), false).isPresent()
                    && getIndexFromName(f.getObject(), false).isPresent()) {
                if (f.getVerb().equals("student_error_more_precedence_base")) {
                    if (getIndexFromName(f.getSubject(), false).orElse(0) > getIndexFromName(f.getObject(), false).orElse(0)) {
                        result.add("error_base_higher_precedence_left");
                    } else {
                        result.add("error_base_higher_precedence_right");
                    }
                } else if (f.getVerb().equals("student_error_left_assoc_base")) {
                    result.add("error_base_same_precedence_left_associativity_left");
                } else if (f.getVerb().equals("student_error_right_assoc_base")) {
                    result.add("error_base_same_precedence_right_associativity_right");
                } else if (f.getVerb().equals("student_error_strict_operands_order_base")) {
                    result.add("error_base_student_error_strict_operands_order");
                } else if (f.getVerb().equals("student_error_unevaluated_operand_base")) {
                    result.add("error_base_student_error_unevaluated_operand");
                } else if (f.getVerb().equals("student_error_early_finish_base")) {
                    result.add("error_base_student_error_early_finish");
                } else if (f.getVerb().equals("student_error_in_complex_base")) {
                    result.add("error_base_student_error_in_complex");

                    // check subject's text
                    boolean is_error_base_enclosing_operators = false;
                    for (BackendFactEntity fa : solutionFacts) {
                        if (fa.getVerb().equals("text") && fa.getSubject().equals(f.getSubject())) {
                            is_error_base_enclosing_operators = List.of("(", "[", "?").contains(fa.getObject());
                            break;
                        }
                    }
                    if (is_error_base_enclosing_operators) {
                        result.add("error_base_enclosing_operators");
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Set<Set<String>> possibleViolationsByStep(Question q, List<ResponseEntity> completedSteps) {
        return new HashSet<>();
    }

    @Override
    public boolean needSupplementaryQuestion(ViolationEntity violation) {
        if (violation.getLawName().equals("error_base_student_error_in_complex") ||
                violation.getLawName().equals("error_base_student_error_strict_operands_order") ||
                violation.getLawName().equals("error_base_student_error_unevaluated_operand") ||
                violation.getLawName().equals("error_base_student_error_early_finish")) {
            return false;
        }
        return true;
    }

    @Override
    public Question makeSupplementaryQuestion(QuestionEntity question, ViolationEntity violation, Language userLang) {
        if (!needSupplementaryQuestion(violation)) {
            return null;
        }

        HashSet<String> targetConcepts = new HashSet<>();
        String failedLaw = violation.getLawName().startsWith("error_base") ? "first_OrderOperatorsSupplementary" : violation.getLawName();
        targetConcepts.add(failedLaw);
        targetConcepts.add("supplementary");

        ExerciseAttemptEntity exerciseAttemptEntity = question.getExerciseAttempt();
        if (exerciseAttemptEntity == null) {
            return null;
        }
        if (!supplementaryConfig.containsKey(failedLaw)) {
            return null;
        }

        Question res = findQuestion(new ArrayList<>(), targetConcepts, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
        if (res != null) {
            Question copy = makeQuestionCopy(res, exerciseAttemptEntity, userLang);
            return fillSupplementaryAnswerObjects(question, failedLaw, copy, userLang);
        }

        return null;
    }

    Question fillSupplementaryAnswerObjects(QuestionEntity originalQuestion, String failedLaw, Question supplementaryQuestion, Language lang) {
        Map<String, List<String>> before = new HashMap<>();
        MultiValuedMap<String, String> beforeIndirect = new HashSetValuedHashMap<>();
        Map<String, String> texts = new HashMap<>();
        Map<String, String> indexes = new HashMap<>();
        MultiValuedMap<String, String> highPrecedence = new HashSetValuedHashMap<>();
        MultiValuedMap<String, String> samePrecedenceLeftAssoc = new HashSetValuedHashMap<>();
        MultiValuedMap<String, String> samePrecedenceRightAssoc = new HashSetValuedHashMap<>();
        HashSet<String> used = new HashSet<>();

        for (BackendFactEntity fact : originalQuestion.getSolutionFacts()) {
            if (fact.getVerb().equals("before_direct")) {
                if (!before.containsKey(fact.getObject())) {
                    before.put(fact.getObject(), new ArrayList<>());
                }
                before.get(fact.getObject()).add(fact.getSubject());
            } else if (fact.getVerb().equals("before")) {
                beforeIndirect.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("text")) {
                texts.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("index")) {
                indexes.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("high_precedence_diff_precedence")) {
                highPrecedence.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("high_precedence_left_assoc")) {
                samePrecedenceLeftAssoc.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("high_precedence_right_assoc")) {
                samePrecedenceRightAssoc.put(fact.getSubject(), fact.getObject());
            }
        }

        AnswerObjectEntity failedAnswer = null;
        if (originalQuestion.getInteractions() == null || originalQuestion.getInteractions().size() == 0) {
            return null;
        }
        InteractionEntity interaction = originalQuestion.getInteractions().get(originalQuestion.getInteractions().size() - 1);
        for (ResponseEntity response : interaction.getResponses()) {
            used.add(response.getLeftAnswerObject().getDomainInfo());
            failedAnswer = response.getLeftAnswerObject();
        }

        if (failedAnswer == null) {
            return null;
        }

        List<BackendFactEntity> possibleViolationFacts = new ArrayList<>();
        {
            BackendFactEntity factOriginalMistake = new BackendFactEntity("","","original_mistake", "", failedLaw);
            possibleViolationFacts.add(factOriginalMistake);
        }

        Integer failedIndex = Integer.parseInt(indexes.get(failedAnswer.getDomainInfo()));

        HashMap<String, String> templates = new HashMap<>();
        for (AnswerObjectEntity origAnswer : originalQuestion.getAnswerObjects()) {
            if (origAnswer.getDomainInfo().equals("end_token")) {
                continue;
            }
            String text = texts.get(origAnswer.getDomainInfo());
            Integer index = Integer.parseInt(indexes.get(origAnswer.getDomainInfo()));
            String domainInfo = origAnswer.getDomainInfo();

            possibleViolationFacts.add(new BackendFactEntity(String.valueOf(origAnswer.getAnswerId()), "text", text));
            possibleViolationFacts.add(new BackendFactEntity(String.valueOf(origAnswer.getAnswerId()), "index", String.valueOf(index)));

            if (origAnswer.getDomainInfo().equals(failedAnswer.getDomainInfo())) {
                templates.put("operator", text);
                templates.put("pos", index.toString());
                templates.put("operator_domain_info", domainInfo);
            }

            if (index < failedIndex && !used.contains(origAnswer.getDomainInfo())) {
                templates.put("left_operator", text);
                templates.put("left_operator_pos", index.toString());
                templates.put("left_operator_domain_info", domainInfo);

                if (highPrecedence.containsMapping(failedAnswer.getDomainInfo(), origAnswer.getDomainInfo())) {
                    templates.put("left_operator_priority", "low");
                    templates.put("left_operator_correct", "wrong");
                } else if (highPrecedence.containsMapping(origAnswer.getDomainInfo(), failedAnswer.getDomainInfo())) {
                    templates.put("left_operator_priority", "high");
                    templates.put("left_operator_correct", "correct");
                } else if (samePrecedenceLeftAssoc.containsMapping(origAnswer.getDomainInfo(), failedAnswer.getDomainInfo())) {
                    templates.put("left_operator_priority", "same");
                    templates.put("left_operator_associativity", "L");
                    templates.put("left_operator_correct", "correct");
                } else {
                    templates.put("left_operator_priority", "same");
                    templates.put("left_operator_associativity", "R");
                    templates.put("left_operator_correct", "wrong");
                }
            }
            if (index > failedIndex && !used.contains(origAnswer.getDomainInfo()) && !templates.containsKey("right_operator")) {
                templates.put("right_operator", text);
                templates.put("right_operator_pos", index.toString());
                templates.put("right_operator_domain_info", domainInfo);

                if (highPrecedence.containsMapping(origAnswer.getDomainInfo(), failedAnswer.getDomainInfo())) {
                    templates.put("right_operator_priority", "high");
                    templates.put("right_operator_correct", "correct");
                } else if (highPrecedence.containsMapping(failedAnswer.getDomainInfo(), origAnswer.getDomainInfo())) {
                    templates.put("right_operator_priority", "low");
                    templates.put("right_operator_correct", "wrong");
                } else if (samePrecedenceRightAssoc.containsMapping(origAnswer.getDomainInfo(), failedAnswer.getDomainInfo())) {
                    templates.put("right_operator_priority", "same");
                    templates.put("right_operator_associativity", "R");
                    templates.put("right_operator_correct", "correct");
                } else {
                    templates.put("right_operator_priority", "same");
                    templates.put("right_operator_associativity", "L");
                    templates.put("right_operator_correct", "wrong");
                }
            }
            //TODO: check in parenthesis left/right
            //TODO: check is failed complex beginning and have inner unused
            //TODO: check operator with strict order
        }

        StringSubstitutor stringSubstitutor = new StringSubstitutor(templates);
        stringSubstitutor.setEnableUndefinedVariableException(true);

        try {
            String text = stringSubstitutor.replace(supplementaryQuestion.getQuestionText());
            text = text.replaceAll(getMessage("SAME_PRECEDENCE_TEMPLATE", lang), getMessage("OPERATOR_TEMPLATE", lang));

            supplementaryQuestion.getQuestionData().setQuestionText(text);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        boolean sameAnswers = false;
        HashSet<String> answerTexts = new HashSet<>();
        List<AnswerObjectEntity> answers = new ArrayList<>();
        for (AnswerObjectEntity answer : supplementaryQuestion.getAnswerObjects()) {
            try {
                String result = stringSubstitutor.replace(answer.getHyperText());
                if (answerTexts.contains(result)) {
                    sameAnswers = true;
                }

                AnswerObjectEntity newAnswer = new AnswerObjectEntity();
                answerTexts.add(result);
                newAnswer.setHyperText(result);
                newAnswer.setAnswerId(answer.getAnswerId());

                List<SupplementaryAnswerTransition> transitions = supplementaryConfig.get(failedLaw).get(answer.getDomainInfo());

                boolean isAnswerCorrect = false;
                for (SupplementaryAnswerTransition transition : transitions) {
                    String[] transitionCheckParts = transition.check.split(";");
                    boolean transitionSuit = false;
                    if (transition.check.equals("correct")) {
                        transitionSuit = true;
                    } else if (transitionCheckParts.length == 3) {
                        String subject = templates.get(transitionCheckParts[0] + "_domain_info");
                        String object = templates.get(transitionCheckParts[2] + "_domain_info");
                        if (transitionCheckParts[1].equals("before")) {
                            transitionSuit = beforeIndirect.containsMapping(subject, object);
                        } else if (transitionCheckParts[1].equals("high_precedence")) {
                            transitionSuit = highPrecedence.containsMapping(subject, object);
                        } else if (transitionCheckParts[1].equals("same_precedence")) {
                            transitionSuit = samePrecedenceLeftAssoc.containsMapping(subject, object) || samePrecedenceRightAssoc.containsMapping(subject, object)
                                    || samePrecedenceLeftAssoc.containsMapping(object, subject) || samePrecedenceRightAssoc.containsMapping(object, subject);
                        } else if (transitionCheckParts[1].equals("same_precedence_left_assoc")) {
                            transitionSuit = samePrecedenceLeftAssoc.containsMapping(subject, object);
                        } else if (transitionCheckParts[1].equals("same_precedence_right_assoc")) {
                            transitionSuit = samePrecedenceRightAssoc.containsMapping(subject, object);
                        } else {
                            throw new IllegalStateException("Supplementary answer correctness check verb is not supported");
                        }
                    }

                    if (transitionSuit) {
                        newAnswer.setDomainInfo(transition.question);
                        isAnswerCorrect = transition.correct;
                        if (!isAnswerCorrect) {
                            BackendFactEntity mistake = new BackendFactEntity(String.valueOf(answer.getAnswerId()), "detailed_law", transition.detailed_law);
                            possibleViolationFacts.add(mistake);
                        }
                        break;
                    }
                }

                if (newAnswer.getDomainInfo() == null) {
                    throw new IllegalStateException("Supplementary answer correctness check failed");
                }

                // skip question with same answers
                if (sameAnswers && isAnswerCorrect) {
                    ViolationEntity violationEntity = new ViolationEntity();
                    violationEntity.setLawName(newAnswer.getDomainInfo());
                    return makeSupplementaryQuestion(originalQuestion, violationEntity, lang);
                }

                answers.add(newAnswer);
            } catch (IllegalArgumentException ex) {
                // pass, this variant should not be used
            }
        }
        supplementaryQuestion.setAnswerObjects(answers);
        supplementaryQuestion.getQuestionData().setSolutionFacts(possibleViolationFacts);
        return supplementaryQuestion;
    }

    @Override
    public InterpretSentenceResult judgeSupplementaryQuestion(Question question, AnswerObjectEntity answer) {
        InterpretSentenceResult interpretSentenceResult = new InterpretSentenceResult();

        interpretSentenceResult.violations = new ArrayList<>();
        interpretSentenceResult.isAnswerCorrect = true;

        ViolationEntity violationEntity = new ViolationEntity();
        if (answer.getDomainInfo() != null) {
            violationEntity.setLawName(answer.getDomainInfo());
        }

        List<BackendFactEntity> violationFacts = new ArrayList<>();
        violationFacts.addAll(question.getSolutionFacts());

        for (BackendFactEntity fact : question.getSolutionFacts()) {
            if (fact.getSubject().equals(String.valueOf(answer.getAnswerId()))) {
                if (fact.getVerb().equals("detailed_law")) {
                    violationEntity.setDetailedLawName(fact.getObject());
                    interpretSentenceResult.isAnswerCorrect = false;
                    if (violationEntity.getLawName() == null) {
                        violationEntity.setLawName(fact.getObject());
                    }
                } else if (fact.getVerb().equals("text") || fact.getVerb().equals("index")) {
                    violationFacts.add(fact);
                }
            }
        }

        violationFacts.addAll(question.getSolutionFacts());
        violationEntity.setViolationFacts(violationFacts);
        if (violationEntity.getLawName() != null || violationEntity.getDetailedLawName() != null) {
            interpretSentenceResult.violations.add(violationEntity);
        }

        return interpretSentenceResult;
    }

    @Override
    public InterpretSentenceResult interpretSentence(Collection<Fact> violations) {
        List<ViolationEntity> mistakes = new ArrayList<>();

        String questionType = "";
        for (Fact violation : violations) {
            if (violation.getVerb().equals("student_operand_type")) {
                questionType = OPERANDS_TYPE_QUESTION_TYPE;
            } else if (violation.getVerb().equals("student_precedence_type")) {
                questionType = PRECEDENCE_TYPE_QUESTION_TYPE;
            } else if (violation.getVerb().equals("student_error_more_precedence")
                    || violation.getVerb().equals("student_error_left_assoc")
                    || violation.getVerb().equals("student_error_right_assoc")
                    || violation.getVerb().equals("student_error_strict_operands_order")
                    || violation.getVerb().equals("student_error_unevaluated_operand")
                    || violation.getVerb().equals("student_error_early_finish")
                    || violation.getVerb().equals("student_error_in_complex")) {
                questionType = EVALUATION_ORDER_QUESTION_TYPE;
            }
        }

        if (questionType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            // retrieve subjects' info from facts ...
            Map<String, Fact> nameToText = new HashMap<>();
            Map<String, Fact> nameToPos = new HashMap<>();
            Map<String, Fact> nameToBeforeThirdOperator = new HashMap<>();

            for (Fact violation : violations) {
                if (violation.getVerb().equals("text")) {
                    nameToText.put(violation.getSubject(), violation);
                } else if (violation.getVerb().equals("index")) {
                    nameToPos.put(violation.getSubject(), violation);
                } else if (violation.getVerb().equals("before_third_operator")) {
                    nameToBeforeThirdOperator.put(violation.getSubject(), violation);
                }
            }

            // filter facts and fill mistakes list ...
            for (Fact violation : violations) {
                ViolationEntity violationEntity = new ViolationEntity();
                if (violation.getVerb().equals("student_error_more_precedence")) {
                    if (getIndexFromName(violation.getSubject(), false).orElse(0) > getIndexFromName(violation.getObject(), false).orElse(0)) {
                        violationEntity.setLawName("error_base_higher_precedence_left");
                    } else {
                        violationEntity.setLawName("error_base_higher_precedence_right");
                    }
                } else if (violation.getVerb().equals("student_error_left_assoc")) {
                    violationEntity.setLawName("error_base_same_precedence_left_associativity_left");
                } else if (violation.getVerb().equals("student_error_right_assoc")) {
                    violationEntity.setLawName("error_base_same_precedence_right_associativity_right");
                } else if (violation.getVerb().equals("student_error_strict_operands_order")) {
                    violationEntity.setLawName("error_base_student_error_strict_operands_order");
                } else if (violation.getVerb().equals("student_error_unevaluated_operand")) {
                    violationEntity.setLawName("error_base_student_error_unevaluated_operand");
                } else if (violation.getVerb().equals("student_error_early_finish")) {
                    violationEntity.setLawName("error_base_student_error_early_finish");
                } else if (violation.getVerb().equals("student_error_in_complex")) {
                    violationEntity.setLawName("error_base_student_error_in_complex");
                } else if (violation.getVerb().equals("wrong_type")) {
                    violationEntity.setLawName("error_wrong_type");
                }
                if (violationEntity.getLawName() != null) {
                    ArrayList<Fact> facts = new ArrayList<>(Arrays.asList(
                            violation,
                            nameToText.get(violation.getObject()),
                            nameToText.get(violation.getSubject()),
                            nameToPos.get(violation.getObject()),
                            nameToPos.get(violation.getSubject())));
                    if (nameToBeforeThirdOperator.containsKey(violation.getObject())) {
                        facts.add(nameToBeforeThirdOperator.get(violation.getObject()));
                        facts.add(nameToPos.get(nameToBeforeThirdOperator.get(violation.getObject()).getObject()));
                        facts.add(nameToText.get(nameToBeforeThirdOperator.get(violation.getObject()).getObject()));
                    }
                    violationEntity.setViolationFacts(Fact.factsToEntities(facts));
                    mistakes.add(violationEntity);
                }
            }
        } else if (questionType.equals(OPERANDS_TYPE_QUESTION_TYPE)) {
            MultiValuedMap<String, String> operatorToOperands = new HashSetValuedHashMap<>();
            MultiValuedMap<String, String> operatorToOperandsParts = new HashSetValuedHashMap<>();
            Map<String, String> studentOperatorType = new HashMap<>();
            Map<String, String> operatorType = new HashMap<>();
            Map<String, Integer> nameToPos = new HashMap<>();
            Set<String> complexOperators = new HashSet<>();
            String targetOperator = "";

            for (Fact violation : violations) {
                if (!violation.getSubject().startsWith("op__0")) {
                    continue;
                }
                if (violation.getVerb().equals("student_operand_type")) {
                    studentOperatorType.put(violation.getSubject(), violation.getObject());
                } else if (violation.getVerb().equals("has_operand")) {
                    operatorToOperands.put(violation.getSubject(), violation.getObject());
                } else if (violation.getVerb().equals("has_operand_part")) {
                    operatorToOperandsParts.put(violation.getSubject(), violation.getObject());
                } else if (violation.getVerb().equals("target_operator")) {
                    targetOperator = violation.getSubject();
                } else if (violation.getVerb().equals("complex_beginning") && violation.getObject().equals("true")) {
                    complexOperators.add(violation.getSubject());
                } else if (violation.getVerb().equals("index")) {
                    nameToPos.put(violation.getSubject(), Integer.valueOf(violation.getObject()));
                }
            }

            for (String operator : operatorToOperandsParts.keySet()) {
                if (operator.equals(targetOperator)) {
                    continue;
                }
                if (operatorToOperandsParts.containsMapping(operator, targetOperator)) {
                    operatorType.put(operator, "operator");
                } else if (!operatorToOperandsParts.containsMapping(targetOperator, operator)) {
                    operatorType.put(operator, "unrelated");
                }
            }

            Integer targetPos = nameToPos.get(targetOperator);
            Collection<String> operands = operatorToOperands.get(targetOperator);
            String innerOperand = "";
            if (complexOperators.contains(targetOperator)) {
                int innerOperandPos = 1000000;
                for (String operand : operands) {
                    Integer pos = nameToPos.get(operand);
                    if (pos > targetPos && pos < innerOperandPos) {
                        innerOperandPos = pos;
                        innerOperand = operand;
                    }
                }
                operatorType.put(innerOperand, "inner_operand");
                for (String innerOperandPart : operatorToOperandsParts.get(innerOperand)) {
                    operatorType.put(innerOperandPart, "inner_operand");
                }
            }
            for (String operand : operands) {
                Integer pos = nameToPos.get(operand);
                if (pos < targetPos) {
                    operatorType.put(operand, "left_operand");
                    for (String operandPart : operatorToOperandsParts.get(operand)) {
                        operatorType.put(operandPart, "left_operand");
                    }
                } if (pos > targetPos && !operand.equals(innerOperand)) {
                    operatorType.put(operand, "right_operand");
                    for (String operandPart : operatorToOperandsParts.get(operand)) {
                        operatorType.put(operandPart, "right_operand");
                    }
                }
            }

            for (Map.Entry<String, String> kv : studentOperatorType.entrySet()) {
                if (!operatorType.get(kv.getKey()).equals(kv.getValue())) {
                    ViolationEntity violationEntity = new ViolationEntity();
                    violationEntity.setLawName("wrong_operand_type");
                    ArrayList<BackendFactEntity> violationFacts = new ArrayList<>();
                    violationFacts.add(new BackendFactEntity(kv.getKey(), "student_operand_type", kv.getValue()));
                    violationFacts.add(new BackendFactEntity(kv.getKey(), "real_operand_type", operatorType.get(kv.getKey())));
                    violationFacts.add(new BackendFactEntity(kv.getKey(), "index", String.valueOf(nameToPos.get(kv.getKey()))));
                    violationEntity.setViolationFacts(new ArrayList<>() /* violationFacts ??*/);
                    mistakes.add(violationEntity);
                }
            }
        } else if (questionType.equals(PRECEDENCE_TYPE_QUESTION_TYPE)) {
            MultiValuedMap<String, String> morePrecedence = new HashSetValuedHashMap<>();
            Map<String, String> studentPrecedenceType = new HashMap<>();
            Map<String, String> precedenceType = new HashMap<>();
            Map<String, Integer> nameToPos = new HashMap<>();
            String targetOperator = "";

            for (Fact violation : violations) {
                if (!violation.getSubject().startsWith("op__0")) {
                    continue;
                }
                if (violation.getVerb().equals("high_precedence_diff_precedence")) {
                    morePrecedence.put(violation.getObject(), violation.getSubject());
                } else if (violation.getVerb().equals("student_precedence_type")) {
                    studentPrecedenceType.put(violation.getSubject(), violation.getObject());
                } else if (violation.getVerb().equals("target_operator")) {
                    targetOperator = violation.getSubject();
                } else if (violation.getVerb().equals("index")) {
                    nameToPos.put(violation.getSubject(), Integer.valueOf(violation.getObject()));
                }
            }

            for (String operator : nameToPos.keySet()) {
                if (morePrecedence.containsMapping(targetOperator, operator)) {
                    precedenceType.put(operator, "higher_precedence");
                } else if (morePrecedence.containsMapping(operator, targetOperator)) {
                    precedenceType.put(operator, "lower_precedence");
                } else {
                    precedenceType.put(operator, "same_precedence");
                }
            }

            for (Map.Entry<String, String> kv : studentPrecedenceType.entrySet()) {
                if (!precedenceType.get(kv.getKey()).equals(kv.getValue())) {
                    ViolationEntity violationEntity = new ViolationEntity();
                    violationEntity.setLawName("wrong_precedence_type");
                    ArrayList<BackendFactEntity> violationFacts = new ArrayList<>();
                    violationFacts.add(new BackendFactEntity(kv.getKey(), "student_precedence_type", kv.getValue()));
                    violationFacts.add(new BackendFactEntity(kv.getKey(), "real_precedence_type", precedenceType.get(kv.getKey())));
                    violationFacts.add(new BackendFactEntity(kv.getKey(), "index", String.valueOf(nameToPos.get(kv.getKey()))));
                    violationEntity.setViolationFacts(new ArrayList<>());
                    mistakes.add(violationEntity);
                }
            }
        }

        InterpretSentenceResult result = new InterpretSentenceResult();
        result.violations = mistakes;
        result.correctlyAppliedLaws = calculateCorrectlyAppliedLaws(violations);
        result.isAnswerCorrect = mistakes.isEmpty();

        ProcessSolutionResult processResult = processSolution(violations);
        result.CountCorrectOptions = processResult.CountCorrectOptions;
        result.IterationsLeft = processResult.IterationsLeft + (result.isAnswerCorrect ? 0 : 1);
        return result;
    }

    List<String> calculateCorrectlyAppliedLaws(Collection<Fact> violations) {
        List<String> result = new ArrayList<>();

        Map<String, Integer> nameToStudentPos = new HashMap<>();
        Integer maxStudentPos = -1;
        for (Fact violation : violations) {
            if (violation.getVerb().equals("student_pos_number")) {
                Integer studentPosNumber = Integer.parseInt(violation.getObject());
                maxStudentPos = max(maxStudentPos, studentPosNumber);
                nameToStudentPos.put(violation.getSubject(), studentPosNumber);
            }
        }

        for (Fact violation : violations) {
            // Consider only errors that could happen at current step and where current token will be error reason
            if (!nameToStudentPos.getOrDefault(violation.getObject(), -2).equals(maxStudentPos) ||
                    nameToStudentPos.containsKey(violation.getSubject())) {
                continue;
            }
            String correctlyAppliedLaw = null;
            if (violation.getVerb().equals("student_error_more_precedence_base")) {
                if (getIndexFromName(violation.getSubject(), false).orElse(0) > getIndexFromName(violation.getObject(), false).orElse(0)) {
                    correctlyAppliedLaw = "error_base_higher_precedence_left";
                } else {
                    correctlyAppliedLaw = "error_base_higher_precedence_right";
                }
            } else if (violation.getVerb().equals("student_error_left_assoc_base")) {
                correctlyAppliedLaw = "error_base_same_precedence_left_associativity_left";
            } else if (violation.getVerb().equals("student_error_right_assoc_base")) {
                correctlyAppliedLaw = "error_base_same_precedence_right_associativity_right";
            } else if (violation.getVerb().equals("student_error_in_complex_base")) {
                correctlyAppliedLaw = "error_base_student_error_in_complex";
            } else if (violation.getVerb().equals("student_error_strict_operands_order_base")) {
                correctlyAppliedLaw = "error_base_student_error_strict_operands_order";
            } else if (violation.getVerb().equals("student_error_unevaluated_operand_base")) {
                correctlyAppliedLaw = "error_base_student_error_unevaluated_operand";
            } else if (violation.getVerb().equals("student_error_early_finish_base")) {
                correctlyAppliedLaw = "error_base_student_error_early_finish";
            }
            if (correctlyAppliedLaw != null) {
                result.add(correctlyAppliedLaw);
            }
        }

        return result;
    }

    @Override
    public List<HyperText> makeExplanation(List<ViolationEntity> mistakes, FeedbackType feedbackType, Language lang) {
        ArrayList<HyperText> result = new ArrayList<>();
        for (ViolationEntity mistake : mistakes) {
            result.add(makeExplanation(mistake, feedbackType, lang));
        }
        return result;
    }

    private String getOperatorTextDescription(String errorText, Language lang) {
        if (errorText.equals("(")) {
            return getMessage("PARENTHESIS", lang);
        } else if (errorText.equals("[")) {
            return getMessage("BRACKETS", lang);
        } else if (errorText.contains("(")) {
            return getMessage("FUNC_CALL", lang);
        }
        return getMessage("OPERATOR", lang);
    }

    private HyperText makeExplanation(ViolationEntity mistake, FeedbackType feedbackType, Language lang) {
        if (mistake.getLawName().equals("error_select_precedence_or_associativity")) {
            return new HyperText(getMessage("ERROR_PRECEDENCE_BEFORE_ASSOC", lang));
        } else if (mistake.getLawName().equals("error_select_highest_precedence")) {
            String text = "";
            String index;
            for (BackendFactEntity fact : mistake.getViolationFacts()) {
                if (fact.getVerb().equals("text")) {
                    text = fact.getObject();
                } else if (fact.getVerb().equals("index")) {
                    index = fact.getObject();
                }
            }
            return new HyperText(getMessage("ERROR_PRECEDENCE_HIGHER1", lang) + text + getMessage("ERROR_PRECEDENCE_HIGHER2", lang));
        } else if (mistake.getLawName().equals("wrong_operand_type")) {
            String realType = "";
            String studentType = "";
            String index = "";
            for (BackendFactEntity fact : mistake.getViolationFacts()) {
                if (fact.getVerb().equals("student_operand_type")) {
                    studentType = fact.getObject();
                } else if (fact.getVerb().equals("real_operand_type")) {
                    realType = fact.getObject();
                } else if (fact.getVerb().equals("index")) {
                    index = fact.getObject();
                }
            }
            return new HyperText("Wrong, operand type of operator at pos " + index + " is '" + realType + "', not '" + studentType + "'");
        }  else if (mistake.getLawName().equals("wrong_precedence_type")) {
            String realType = "";
            String studentType = "";
            String index = "";
            for (BackendFactEntity fact : mistake.getViolationFacts()) {
                if (fact.getVerb().equals("student_precedence_type")) {
                    studentType = fact.getObject();
                } else if (fact.getVerb().equals("real_precedence_type")) {
                    realType = fact.getObject();
                } else if (fact.getVerb().equals("index")) {
                    index = fact.getObject();
                }
            }
            return new HyperText("Wrong, precedence type of operator at pos " + index + " is '" + realType + "', not '" + studentType + "'");
        }

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

        StringJoiner joiner = new StringJoiner(" ");
        joiner
                .add(getOperatorTextDescription(reasonText, lang))
                .add(reasonText)
                .add(getMessage("AT_POS", lang))
                .add(reasonPos)
                .add(getMessage("EVALUATES_BEFORE", lang))
                .add(getOperatorTextDescription(errorText, lang))
                .add(errorText)
                .add(getMessage("AT_POS", lang))
                .add(errorPos);
        joiner.add("\n").add(getMessage("BECAUSE", lang));

        String errorType = mistake.getLawName();

        if (errorType.equals("error_base_higher_precedence_left") ||
                errorType.equals("error_base_higher_precedence_right")) {
            joiner
                    .add(getOperatorTextDescription(reasonText, lang))
                    .add(reasonText)
                    .add(getMessage("HAS_HIGHER_PRECEDENCE", lang));
        } else if (errorType.equals("error_base_same_precedence_left_associativity_left") && errorText.equals(reasonText)) {
            joiner
                    .add(getOperatorTextDescription(reasonText, lang))
                    .add(reasonText)
                    .add(getMessage("LEFT_ASSOC_DESC", lang));
        } else if (errorType.equals("error_base_same_precedence_left_associativity_left")) {
            joiner
                    .add(getOperatorTextDescription(reasonText, lang))
                    .add(reasonText)
                    .add(getMessage("SAME_PRECEDENCE_LEFT_ASSOC", lang));
        } else if (errorType.equals("error_base_same_precedence_right_associativity_right") && errorText.equals(reasonText)) {
            joiner
                    .add(getOperatorTextDescription(reasonText, lang))
                    .add(reasonText)
                    .add(getMessage("RIGHT_ASSOC_DESC", lang));
        } else if (errorType.equals("error_base_same_precedence_right_associativity_right")) {
            joiner
                    .add(getOperatorTextDescription(reasonText, lang))
                    .add(reasonText)
                    .add(getMessage("SAME_PRECEDENCE_RIGHT_ASSOC", lang));
        } else if (errorType.equals("error_base_student_error_in_complex") && errorText.equals("(")) {
            joiner.add(getMessage("FUNC_ARGUMENTS_BEFORE_CALL", lang));
        } else if (errorType.equals("error_base_student_error_in_complex") && thirdOperatorText.equals("(")) {
            joiner.add(getMessage("IN_PARENTHESIS_BEFORE", lang));
        } else if (errorType.equals("error_base_student_error_in_complex")) {
            joiner.add(getMessage("IN_COMPLEX_BEFORE", lang));
        } else if (errorType.equals("error_base_student_error_strict_operands_order")) {
            joiner
                    .add(getMessage("LEFT_OPERAND", lang))
                    .add(getOperatorTextDescription(thirdOperatorText, lang))
                    .add(thirdOperatorText)
                    .add(getMessage("AT_POS", lang))
                    .add(thirdOperatorPos)
                    .add(getMessage("MUST_BEFORE_RIGHT", lang));
        } else if (errorType.equals("error_base_student_error_unevaluated_operand")) {
            joiner = new StringJoiner(" ");
            joiner
                    .add(getOperatorTextDescription(errorText, lang))
                    .add(errorText)
                    .add(getMessage("AT_POS", lang))
                    .add(errorPos)
                    .add(getMessage("NOT_EVALUATES", lang));
            joiner.add("\n").add(getMessage("BECAUSE", lang));
            joiner
                    .add(getMessage("LEFT_OPERAND_FOR_OPERATOR", lang))
                    .add(reasonText)
                    .add(getMessage("AT_POS", lang))
                    .add(reasonPos)
                    .add(getMessage("HAS_VALUE_AND_EVALUATE_OTHER_PART", lang)
                            .replaceAll("\\$\\{evaluation_result}", reasonText.equals("&&") ? "false" : "true"));
        } else if (errorType.equals("error_base_student_error_early_finish")) {
            joiner = new StringJoiner(" ");
            joiner
                    .add(getOperatorTextDescription(errorText, lang))
                    .add(errorText)
                    .add(getMessage("AT_POS", lang))
                    .add(errorPos)
                    .add(getMessage("EVALUATES", lang));
            joiner.add("\n").add(getMessage("BECAUSE", lang));
            joiner.add(getMessage("HAS_UNEVALUATED_OPERATOR", lang));
        } else {
            joiner.add(getMessage("UNKNOWN_ERROR", lang));
        }

        return new HyperText(joiner.toString());
    }

    private List<BackendFactEntity> modelToFacts(Model factsModel, boolean onlySolvedFacts) {
        JenaBackend jback = new JenaBackend();
        jback.createOntology(AbstractRdfStorage.NS_code.base());

        // fill model
        OntModel model = jback.getModel();
        model.add(factsModel);

        if (onlySolvedFacts) {
            return jback.getFacts(getViolationVerbs(EVALUATION_ORDER_QUESTION_TYPE, null));
        }
        return jback.getFacts(null);
    }

    public static OntModel factsToOntModel(List<BackendFactEntity> backendFacts) {
        JenaBackend jback = new JenaBackend();
        jback.createOntology(AbstractRdfStorage.NS_code.base());

        OntModel model = jback.getModel();

        jback.addBackendFacts(backendFacts);

        return model;
    }

    @Override
    public Map<String, Model> generateDistinctQuestions(String templateName, Model solvedTemplate, Model domainSchema, int questionsLimit) {
        FactsGraph fg = new FactsGraph(modelToFacts(solvedTemplate, false));

        Map<String,List<BackendFactEntity>> addedFacts = new HashMap<>();

        if (!fg.filterFacts(null, "text", "?:").isEmpty() || !fg.filterFacts(null, "text", "").isEmpty()) {
            return new HashMap<>(); // Skip bad generation of ternary operator
        }
        addedFacts.put(templateName + "_v", new ArrayList<>());

        List<BackendFactEntity> switchPoints = fg.filterFacts(null, "has_value_eval_restriction", null);
        switchPoints.sort(
                Comparator.comparingInt((BackendFactEntity a) -> Integer.parseInt(a.getSubject().substring("op__0__".length()))));
        for (BackendFactEntity branchOperands : switchPoints) {
            List<BackendFactEntity> leftOp = fg.filterFacts(branchOperands.getSubject(), "has_left_operand", null);
            assert 1 == leftOp.size();
            Map<String,List<BackendFactEntity>> newAddedFacts = new HashMap<>();

            List<BackendFactEntity> left_op_is_switch = fg.filterFacts(leftOp.get(0).getObject(), "has_value_eval_restriction", null);
            List<BackendFactEntity> left_op_is_ternary = fg.filterFacts(leftOp.get(0).getObject(), "has_inner_operand", null);
            String left_op_left_op = null;
            if (!left_op_is_switch.isEmpty() && left_op_is_ternary.isEmpty()) {
                left_op_left_op = fg.filterFacts(leftOp.get(0).getObject(), "has_left_operand", null).get(0).getObject();
            }

            for (Map.Entry<String, List<BackendFactEntity>> facts : addedFacts.entrySet()) {
                boolean canBeTrue = true;
                boolean canBeFalse = true;
                if (left_op_left_op != null) {
                    for (BackendFactEntity prevFact : facts.getValue()) {
                        if (left_op_left_op.equals(prevFact.getSubject())) {
                            if (left_op_is_switch.get(0).getObject().equals("no_right_if_true") && prevFact.getObject().equals("true")) {
                                canBeFalse = false;
                            }
                            if (left_op_is_switch.get(0).getObject().equals("no_right_if_false") && prevFact.getObject().equals("false")) {
                                canBeTrue = false;
                            }
                        }
                    }
                }

                // Decrease number of variants in huge questions
                if (canBeTrue && switchPoints.size() > 25 && random() < 0.05) {
                    canBeTrue = false;
                }
                if (canBeFalse && switchPoints.size() > 25 && random() < 0.05) {
                    canBeFalse = false;
                }

                if (canBeTrue) {
                    List<BackendFactEntity> addedTrueFacts = new ArrayList<>();
                    addedTrueFacts.addAll(facts.getValue());
                    addedTrueFacts.add(new BackendFactEntity("owl:NamedIndividual", leftOp.get(0).getObject(), "has_value", "xsd:boolean", "true"));
                    newAddedFacts.put(facts.getKey() + "t", addedTrueFacts);
                }

                if (canBeFalse) {
                    List<BackendFactEntity> addedFalseFacts = new ArrayList<>();
                    addedFalseFacts.addAll(facts.getValue());
                    addedFalseFacts.add(new BackendFactEntity("owl:NamedIndividual", leftOp.get(0).getObject(), "has_value", "xsd:boolean", "false"));
                    newAddedFacts.put(facts.getKey() + "f", addedFalseFacts);
                }
            }
            addedFacts = newAddedFacts;
        }

        System.out.println("Generated: " + addedFacts.size() + " questions");
        HashMap<String, Model> questions = new HashMap<>();
        for (Map.Entry<String, List<BackendFactEntity>> facts : addedFacts.entrySet()) {
            questions.put(facts.getKey(), factsToOntModel(facts.getValue()));
            if (questions.size() > questionsLimit || addedFacts.size() > 10000 && questions.size() >= 5) {
                break;
            }
        }
        return questions;
    }

    /**
     * @param questionName name for the question
     * @param model solved model
     * @param rs instance of Storage
     * @return fresh Ordering Question
     */
    public Question createQuestionFromModel(String questionName, Model model, AbstractRdfStorage rs) {
        List<BackendFactEntity> facts = modelToFacts(model, false);
        facts.add(new BackendFactEntity("owl:NamedIndividual", "end_token", "text", "xsd:string", "end_token"));
        FactsGraph fg = new FactsGraph(facts);

        QuestionEntity entity = new QuestionEntity();
        List<AnswerObjectEntity> answerObjectEntities = new ArrayList<>();
        int ans_id = 0;
        int solution_length = 0;
        Map<Integer, BackendFactEntity> texts = new TreeMap<>(); // expression tokens
        Map<Integer, BackendFactEntity> orderAnswers = new TreeMap<>();

        for (BackendFactEntity token : fg.filterFacts(null, "index", null)) {
            orderAnswers.put(Integer.parseInt(token.getObject()), token);
        }

        for (BackendFactEntity token : orderAnswers.values()) {
            String text = fg.filterFacts(token.getSubject(), "text", null).get(0).getObject();
            BackendFactEntity initFacts = new BackendFactEntity(null,null, null,null,text);

            List<BackendFactEntity> hasValue = fg.filterFacts(token.getSubject(), "has_value", null);
            if (!hasValue.isEmpty()) {
                initFacts.setSubjectType(hasValue.get(0).getObject());
            }

            if (!fg.filterFacts(token.getSubject(), "not_selectable", null).isEmpty()) {
                texts.put(Integer.parseInt(token.getObject()), initFacts);
                continue;
            }

            if (fg.filterFacts(null, "has_uneval_operand", token.getSubject()).isEmpty()) {
                solution_length++;
            }

            initFacts.setSubject("operator");
            texts.put(Integer.parseInt(token.getObject()), initFacts);

            AnswerObjectEntity newAnswerObjectEntity = new AnswerObjectEntity();
            newAnswerObjectEntity.setAnswerId(ans_id);
            ans_id++;
            newAnswerObjectEntity.setConcept("operator");
            newAnswerObjectEntity.setDomainInfo("op__0__" + token.getObject());
            newAnswerObjectEntity.setHyperText(text);
            newAnswerObjectEntity.setQuestion(null);
            newAnswerObjectEntity.setRightCol(false);
            newAnswerObjectEntity.setResponsesLeft(new ArrayList<>());
            newAnswerObjectEntity.setResponsesRight(new ArrayList<>());
            answerObjectEntities.add(newAnswerObjectEntity);
        }
        // Add answer for stop evaluation
        AnswerObjectEntity newAnswerObjectEntity = new AnswerObjectEntity();
        newAnswerObjectEntity.setAnswerId(ans_id);
        newAnswerObjectEntity.setConcept(END_EVALUATION);
        newAnswerObjectEntity.setDomainInfo("end_token");
        newAnswerObjectEntity.setHyperText(END_EVALUATION);
        newAnswerObjectEntity.setQuestion(null);
        newAnswerObjectEntity.setRightCol(false);
        newAnswerObjectEntity.setResponsesLeft(new ArrayList<>());
        newAnswerObjectEntity.setResponsesRight(new ArrayList<>());
        answerObjectEntities.add(newAnswerObjectEntity);
        texts.put(1000000, new BackendFactEntity(null,END_EVALUATION,null,null,END_EVALUATION));

        entity.setAnswerObjects(answerObjectEntities);
        entity.setExerciseAttempt(null);
        entity.setQuestionDomainType("OrderOperators");
        entity.setStatementFacts(facts);
        entity.setSolutionFacts(facts);
        entity.setQuestionType(QuestionType.ORDER);
        entity.setQuestionName("");

        // check the size of question
        if (ans_id < 3 || ans_id > 30)
            // too small or too large question
            return null;

        List<BackendFactEntity> textFacts = new ArrayList<>(texts.values());
        entity.setQuestionText(ExpressionToHtml(textFacts));
//        entity.setQuestionText(ExpressionToHtmlEnablingButtonDuplicates(textFacts));
        entity.setQuestionName(questionName);

        Question question = new Ordering(entity, null);

        Set<String> lawNames = new HashSet<>();
        for (BackendFactEntity fact : fg.filterFacts(null, "law_name", null)) {
            lawNames.add(fact.getObject());
        }
        // question.getPositiveLaws().addAll(lawNames); // no PositiveLaws in question entity

        Set<String> concepts = new HashSet<>();
        for (BackendFactEntity fact : fg.filterFacts(null, "concept", null)) {
            concepts.add(fact.getObject());
        }
        concepts.add("operator");

        question.getConcepts().addAll(concepts);
        Set<String> violations = possibleViolations(question, null);

        /*
        // use Python parser to infer possibly more concepts and violations
        // convert tokens to string omitting last one that is END_EVALUATION
        String exprString = textFacts.stream().map(BackendFactEntity::getObject).takeWhile(s -> !s.equals(END_EVALUATION)).collect(Collectors.joining(" "));
        List<String> concepts_violations = ExpressionSituationPythonCaller.invoke(exprString);
        if (concepts_violations.size() == 2) { // validate the structure
            // show if something new was inferred
            Set<String> moreConcepts = new HashSet<>(List.of(concepts_violations.get(0).split(" ")));
            Set<String> moreViolations = new HashSet<>(List.of(concepts_violations.get(0).split(" ")));

            val newConcepts = new HashSet<>(moreConcepts);
            newConcepts.removeAll(concepts);
            if (!newConcepts.isEmpty()) {
                System.out.println("python sub-service: inferred "+newConcepts.size()+" more concepts: " + newConcepts);
                concepts.addAll(newConcepts);
            }
            val newViolations = new HashSet<>(moreViolations);
            newViolations.removeAll(concepts);
            if (!newViolations.isEmpty()) {
                System.out.println("python sub-service: inferred "+newViolations.size()+" more violations: " + newViolations);
                violations.addAll(newViolations);
            }
        }
        // finished with the results of external Python tool. */

        List<String> tagNames = List.of("basics", "operators", "order", "evaluation", "errors", "C++");

        question.getTags().addAll(tagNames);

        entity.setSolutionFacts(null);

        // make Options instance if absent
        if (entity.getOptions() == null) {
            entity.setOptions(new OrderQuestionOptionsEntity());
        }

        QuestionMetadataDraftEntity meta = rs.findQuestionByName(questionName);
        if (meta == null) {
            meta = rs.createQuestion(questionName, questionName.split("_v")[0], false);
        }
        // QuestionMetadataEntity metadata = entity.getOptions().getMetadata();
        // // entity.getOptions().setMetadata(metadata); // see below

        meta.setDraft(true);  // ordinary metadata instance but this may be useful to indicate it's still "draft", i.e. not yet accepted for import to main table.

        // meta.setName(questionName);
        meta.setDomainShortname(this.getShortName());
        meta.setStage(AbstractRdfStorage.STAGE_READY);  // 3 = generated question
        meta.setVersion(AbstractRdfStorage.GENERATOR_VERSION);  // v10 : generated by Domain
        meta.setUsedCount(0L);
        meta.setDateLastUsed(new Date()); // generated at this time

        question.setTags(new HashSet<>(tagNames));
        meta.setTagBits(tagNames.stream().map(this::getTag).filter(Objects::nonNull).map(Tag::getBitmask).reduce((a,b) -> a|b).orElse(0L));

        // positive only laws
        meta.setLawBits(lawNames.stream().map(this::getPositiveLaw).filter(Objects::nonNull).map(Law::getBitmask).reduce((a,b) -> a|b).orElse(0L));

        question.setNegativeLaws(new ArrayList<>(violations));
        meta.setViolationBits(violations.stream().map(this::getNegativeLaw).filter(Objects::nonNull).map(Law::getBitmask).reduce((a,b) -> a|b).orElse(0L));

        question.setConcepts(new ArrayList<>(concepts));
        meta.setConceptBits(concepts.stream().map(this::getConcept).filter(Objects::nonNull).map(Concept::getBitmask).reduce((a,b) -> a|b).orElse(0L));
        meta.setTraceConceptBits(0L);  // trace concepts (encountered during solving the question) are not important for this domain.

        double complexity = 0.18549906 * solution_length - 0.01883239 * violations.size();
        double integralComplexity = 1/( 1 + Math.exp(-1*complexity));


        // add metadata to question (later it may be stored in DB)

        meta.setIntegralComplexity(integralComplexity);
        meta.setSolutionStructuralComplexity((double) ans_id);  // number of clickable operators
        meta.setSolutionSteps(solution_length);
        meta.setDistinctErrorsCount(violations.size());

        // save current state into DB
        meta = rs.saveMetadataDraftEntity(meta);

        // write info to question metadata
        QuestionMetadataEntity metadata = meta.toMetadataEntity();
        entity.getOptions().setMetadata(metadata);
        question.setMetadata(metadata);

        return question;
    }

    public String questionToJson(Question question) {
        return "{\"questionType\": \"ORDERING\", " + new Gson().toJson(question).substring(1);
    }

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

    private HashMap<String, Long> _getTagsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(8);
        name2bit.put("C++", 1L);  	// (2 ^ 0)
        name2bit.put("basics", 2L);  	// (2 ^ 1)
        name2bit.put("errors", 4L);  	// (2 ^ 2)
        name2bit.put("evaluation", 8L);  	// (2 ^ 3)
        name2bit.put("operators", 16L);  	// (2 ^ 4)
        name2bit.put("order", 32L);  	// (2 ^ 5)
        return name2bit;
    }
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
        name2bit.put("operator_function_call", 0x10000000000L);  	// (1099511627776)
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
        HashMap<String, Long> name2bit = new HashMap<>(16);
        name2bit.put("single_token_binary_execution", 0x1L);  	// (1)
        name2bit.put("two_token_binary_execution", 0x2L);  	// (2)
        name2bit.put("single_token_unary_prefix_execution", 0x4L);  	// (4)
        name2bit.put("two_token_unary_execution", 0x8L);  	// (8)
        name2bit.put("single_token_unary_postfix_execution", 0x10L);  	// (16)
        return name2bit;
    }
}
