package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.apache.jena.rdf.model.Model;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionDataEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.exceptions.MeaningTreeException;
import org.vstu.meaningtree.languages.CppTranslator;
import org.vstu.meaningtree.languages.LanguageTranslator;
import org.vstu.meaningtree.nodes.Node;
import org.vstu.meaningtree.nodes.expressions.bitwise.*;
import org.vstu.meaningtree.nodes.expressions.calls.FunctionCall;
import org.vstu.meaningtree.nodes.expressions.comparison.*;
import org.vstu.meaningtree.nodes.expressions.logical.ShortCircuitAndOp;
import org.vstu.meaningtree.nodes.expressions.logical.ShortCircuitOrOp;
import org.vstu.meaningtree.nodes.expressions.math.*;
import org.vstu.meaningtree.nodes.expressions.other.*;
import org.vstu.meaningtree.nodes.expressions.pointers.PointerMemberAccess;
import org.vstu.meaningtree.nodes.expressions.pointers.PointerPackOp;
import org.vstu.meaningtree.nodes.expressions.pointers.PointerUnpackOp;
import org.vstu.meaningtree.nodes.expressions.unary.UnaryMinusOp;
import org.vstu.meaningtree.nodes.expressions.unary.UnaryPlusOp;
import org.vstu.meaningtree.nodes.statements.ExpressionSequence;
import org.vstu.meaningtree.serializers.rdf.RDFDeserializer;
import org.vstu.meaningtree.serializers.rdf.RDFSerializer;
import org.vstu.meaningtree.utils.tokens.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MeaningTreeOrderQuestionBuilder {
    /***
     * Построитель сериализованного вопроса SerializableQuestion, пригодного для сохранения в хранилище
     * Выражение может быть построено на любом поддерживаемом языке программирования
     * и преобразовано в другой поддерживаемый язык программирования с помощью использования общего дерева языков проекта MeaningTree
     *
     * Представляет собой прослойку, пригодную для генерирования и выдачи уже существующих вопросов на любом поддерживаемом языке программирования
     */

    // Входные данные: существующий вопрос или новое выражение
    protected QuestionDataEntity source = null; // существующий вопрос
    protected MeaningTree sourceExpressionTree = null; // новое выражение

    // Дополнительные входные данные для нового вопроса: источник вопроса
    protected String questionOrigin = ""; // источник вопроса для метаданных

    // Домен, для которого выполняется преобразование
    protected ProgrammingLanguageExpressionDTDomain domain;

    // Данные, получаемые на первом этапе
    protected TokenList tokens; // токены сконвертированного выражения
    protected String rawTranslatedCode; // код сконвертированного выражения

    // Данные, из которых сформируется вопрос
    protected List<SerializableQuestion.StatementFact> stmtFacts;
    protected List<SerializableQuestion.AnswerObject> answerObjects;
    protected SerializableQuestion.QuestionMetadata metadata;
    protected SerializableQuestion.QuestionData qdata;
    protected List<String> tags;
    protected Set<String> concepts;
    protected Set<String> possibleViolations;

    protected boolean bulkMode = false; // Определяет генерировать ли максимально независимый от языка вопрос (только метаданные будут зависеть)

    protected static final int MIN_VERSION = 12;
    protected static final int TARGET_VERSION = 12;

    private static final List<String> defaultTags = new ArrayList<>(List.of("basics", "operators", "order", "evaluation", "errors"));

    static {
        for (String lang : SupportedLanguage.getStringMap().keySet()) {
            defaultTags.add(lang.substring(0, 1).toUpperCase() + lang.substring(1));
        }
    }

    MeaningTreeOrderQuestionBuilder(ProgrammingLanguageExpressionDTDomain domain) {
        this.domain = domain;
    }

    /***
     * Построить новый вопрос из старого с помощью MeaningTree
     * @param q - сериализованный вопрос
     * @param domain - поддерживаемый домен
     * @return построитель вопроса
     */
    public static MeaningTreeOrderQuestionBuilder fromExistingQuestion(SerializableQuestion q, ProgrammingLanguageExpressionDTDomain domain) {
        MeaningTree mt;
        if (q.getMetadata().getVersion() >= MIN_VERSION) {
            mt = MeaningTreeRDFHelper.backendFactsToMeaningTree(
                    MeaningTreeRDFHelper.serializableToBackendFacts(q.getQuestionData().getStatementFacts())
            );
        } else {
            mt = extractExpression(q.getQuestionData().getStatementFacts());
        }
        MeaningTreeOrderQuestionBuilder builder = new MeaningTreeOrderQuestionBuilder(domain);
        builder.sourceExpressionTree = mt;
        builder.questionOrigin(q.getMetadata().getOrigin());
        return builder;
    }

    protected static MeaningTree extractExpression(List<SerializableQuestion.StatementFact> facts) {
        CppTranslator cppTranslator = new CppTranslator(new HashMap<>() {{
            put("skipErrors", "true");
            put("expressionMode", "true");
            put("translationUnitMode", "false");
        }});
        StringBuilder tokenBuilder = new StringBuilder();

        Map<Integer, String> indexes = new HashMap<>();
        Map<String, String> tokenValues = new HashMap<>();
        Map<String, Boolean> semanticValues = new HashMap<>();

        for (SerializableQuestion.StatementFact st : facts) {
            if (st.getVerb().equals("index")) {
                indexes.put(Integer.parseInt(st.getObject()), st.getSubject());
            } else if (st.getVerb().equals("text")) {
                tokenValues.put(st.getSubject(), st.getObject());
            } else if (st.getVerb().equals("has_value")) {
                semanticValues.put(st.getSubject(), Boolean.valueOf(st.getObject()));
            }
        }
        for (int i = 0; i <= indexes.keySet().stream().max(Long::compare).orElse(0); i++) {
            if (indexes.containsKey(i)) {
                tokenBuilder.append(tokenValues.get(indexes.get(i)));
                tokenBuilder.append(' ');
            }
        }
        String tokens = tokenBuilder.substring(0, tokenBuilder.length() - 1);
        TokenList tokenList = cppTranslator.getTokenizer().tokenizeExtended(cppTranslator.prepareCode(tokens));
        HashMap<TokenGroup, Object> semanticValuesIndexes = new HashMap<>();
        for (int i = 0; i < indexes.keySet().stream().max(Long::compare).orElse(0); i++) {
            if (semanticValues.containsKey(indexes.get(i))) {
                semanticValuesIndexes.put(new TokenGroup(i, i + 1, tokenList), semanticValues.get(indexes.get(i)));
            }
        }
        return cppTranslator.getMeaningTree(tokenList, semanticValuesIndexes);
    }



    public static SupportedLanguage detectLanguageFromTags(Collection<String> tags) {
        // Считаем, что в тегах может быть указан только один язык
        List<String> languages = SupportedLanguage.getMap().keySet().stream().map(SupportedLanguage::toString).toList();
        for (String tag : tags) {
            if (languages.contains(tag.toLowerCase())) {
                return SupportedLanguage.fromString(tag.toLowerCase());
            }
        }
        return SupportedLanguage.CPP;
    }

    public static MeaningTreeOrderQuestionBuilder newQuestion(String expression,
                                                              SupportedLanguage language,
                                                              ProgrammingLanguageExpressionDTDomain domain) {
        MeaningTreeOrderQuestionBuilder builder = new MeaningTreeOrderQuestionBuilder(domain);
        try {
            builder.sourceExpressionTree = language.createTranslator(new HashMap<>() {{
                put("skipErrors", "true");
                put("translationUnitMode", "false");
                put("expressionMode", "true");
            }}).getMeaningTree(expression);
            return builder;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Cannot create source translator with ".concat(language.toString()));
        }

    }

    public static List<SerializableQuestion.AnswerObject> generateAnswerObjects(TokenList tokens) {
        List<SerializableQuestion.AnswerObject> result = new ArrayList<>();
        int answerObjId = 0;
        int tokenId = 0;
        for (Token t : tokens) {
            if (t instanceof OperatorToken) {
                result.add(SerializableQuestion.AnswerObject.builder()
                        .answerId(answerObjId)
                        .isRightCol(false)
                        .concept("operator")
                        .hyperText(t.value)
                        .domainInfo(String.format("token_%d", tokenId))
                        .build()
                );
                answerObjId++;
            }
            tokenId++;
        }
        result.add(SerializableQuestion.AnswerObject.builder()
                .answerId(answerObjId)
                .isRightCol(false)
                .concept("student_end_evaluation")
                .hyperText("student_end_evaluation")
                .domainInfo("end_token")
                .build()
        );
        return result;
    }

    public static MeaningTreeOrderQuestionBuilder newQuestion(MeaningTree mt, SupportedLanguage language, ProgrammingLanguageExpressionDTDomain domain) {
        MeaningTreeOrderQuestionBuilder builder = new MeaningTreeOrderQuestionBuilder(domain);
        builder.sourceExpressionTree = mt;
        return builder;
    }

    static Map<SerializableQuestion, List<SerializableQuestion.QuestionMetadata>> separateAll(
            Map<SupportedLanguage, List<SerializableQuestion>> questions
    ) {
        Map<SerializableQuestion, List<SerializableQuestion>> map = questions.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(
                                s -> s.getQuestionData().getStatementFacts(),
                                Collectors.toList()
                        ),
                        groupedByFacts -> groupedByFacts.values().stream()
                                .flatMap(List::stream)
                                .collect(Collectors.groupingBy(Function.identity()))
                ));
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(SerializableQuestion::getMetadata)
                                .collect(Collectors.toList())
                ));
    }

    public static SerializableQuestion fastBuildFromExisting(SerializableQuestion data, SupportedLanguage lang, ProgrammingLanguageExpressionDTDomain domain) {
        return MeaningTreeOrderQuestionBuilder.fromExistingQuestion(data, domain)
                .build(lang)
                .getFirst()
        ;
    }

    public static Question fastBuildFromExisting(Question data, SupportedLanguage lang, ProgrammingLanguageExpressionDTDomain domain) {
        SerializableQuestion q = SerializableQuestion.fromQuestion(data);
        return fastBuildFromExisting(q, lang, domain).toQuestion(domain, q.toMetadataEntity());
    }


    /**
     * Указать источник вопроса (откуда он взят)
     * @param name - название источника
     * @return построитель вопроса
     */
    public MeaningTreeOrderQuestionBuilder questionOrigin(String name) {
        questionOrigin = name;
        return this;
    }


    /**
     * Построить один или несколько вопросов по заданным входным данным
     * Если источником был существующий конкретный вопрос, то результатом также будет один вопрос
     * Если источником было новое выражение на любом языке, то оно будет использовано как шаблон,
     * из которого сгенерируются несколько конкретных вопросов
     * @return один или несколько сериализуемых вопросов
     */
    public List<SerializableQuestion> build(SupportedLanguage language) {
        answerObjects = new ArrayList<>();
        processTemplateStatementFacts();
        return generateManyQuestions(language);
    }

    public Map<SupportedLanguage, List<SerializableQuestion>> buildAll() {
        HashMap<SupportedLanguage, List<SerializableQuestion>> map = new HashMap<>();
        bulkMode = true;
        for (SupportedLanguage language : SupportedLanguage.getMap().keySet()) {
            map.put(language, build(language));
        }
        bulkMode = false;
        return map;
    }

    public Map<SerializableQuestion, List<SerializableQuestion.QuestionMetadata>> buildAllSeparated() {
        return separateAll(buildAll());
    }

    public List<Question> buildQuestion(SupportedLanguage lang) {
        return build(lang).stream().map((SerializableQuestion q) -> q.toQuestion(domain, q.toMetadataEntity())).toList();
    }

    public Map<QuestionDataEntity, List<QuestionMetadataEntity>> buildSeparatedEntities() {
        return buildAllSeparated().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> new QuestionDataEntity(null, entry.getKey()),
                        entry -> entry.getValue().stream()
                                .map(SerializableQuestion::toMetadataEntity)
                                .collect(Collectors.toList())
                ));
    }

    /**
     * Шаг 7: Формирование уникального вопроса из шаблона
     * @return сериализуемый вопрос
     */
    protected SerializableQuestion generateFromTemplate(SupportedLanguage lang, int tokenPos, boolean value) {
        try {
            MeaningTree mt = lang.createTranslator(new HashMap<>() {{
                put("skipErrors", "true");
                put("expressionMode", "true");
                put("translationUnitMode", "false");
            }}).getMeaningTree(tokens, new HashMap<>() {{
                if (tokenPos != -1) {
                    put(new TokenGroup(tokenPos, tokenPos + 1, tokens), value);
                }
            }});
            Model model = new RDFSerializer().serialize(mt.getRootNode());
            processQuestionData(MeaningTreeRDFHelper.backendFactsToSerialized(
                    MeaningTreeRDFHelper.factsFromModel(model)
            ));
            return SerializableQuestion.builder()
                    .questionData(qdata)
                    .concepts(bulkMode ? List.of() : concepts.stream().toList())
                    .negativeLaws(bulkMode ? List.of() : possibleViolations.stream().toList())
                    .metadata(metadata)
                    .tags(bulkMode ? defaultTags : tags)
                    .build();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Failed to create translator");
        }

    }

    /**
     * Шаг 6: Генерирование нескольких вопросов из одного шаблона
     * @return много сериализуемых вопросов из одного шаблона
     */
    protected List<SerializableQuestion> generateManyQuestions(SupportedLanguage language) {
        processTokens(language);
        answerObjects = generateAnswerObjects(tokens);
        processMetadata(language);

        if (tokens.stream().anyMatch((Token t) -> t.getAssignedValue() != null) && source != null) {
            return List.of(generateFromTemplate(language, -1, false));
        }
        List<SerializableQuestion> generated = new ArrayList<>();
        OperandEvaluationMap map = new OperandEvaluationMap(this);
        Map<Integer, Boolean> generatedValues = map.generate();
        for (Map.Entry<Integer, Boolean> entry : generatedValues.entrySet()) {
            generated.add(generateFromTemplate(language, entry.getKey(), entry.getValue()));
        }
        if (generatedValues.isEmpty()) {
            return List.of(generateFromTemplate(language,-1, false));
        }

        return generated;
    }

    /**
     * Шаг 2: Извлечение токенов из выражения
     */
    protected void processTokens(SupportedLanguage language) {
        LanguageTranslator toTranslator;
        try {
            toTranslator = language.createTranslator(new HashMap<String, String>() {{
                put("expressionMode", "true");
                put("skipErrors", "true");
                put("translationUnitMode", "false");
            }});
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Cannot create source translator with ".concat(language.toString()));
        }
        rawTranslatedCode = toTranslator.getCode(sourceExpressionTree);
        tokens = toTranslator.getTokenizer().tokenizeExtended(rawTranslatedCode);
    }

    /**
     * Шаг 7.1: Формирование данных уникального вопроса
     * @param facts - содержимое вопроса
     */
    protected void processQuestionData(List<SerializableQuestion.StatementFact> facts) {
        String questionText = "";
        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showTrace(true)
                .multipleSelectionEnabled(false)
                .showSupplementaryQuestions(true)
                .requireAllAnswers(true)
                .orderNumberOptions(new OrderQuestionOptionsEntity.OrderNumberOptions("#", OrderQuestionOptionsEntity.OrderNumberPosition.BOTTOM, null))
                .build();
        qdata = SerializableQuestion.QuestionData.builder()
                .questionText(questionText)
                .questionName(metadata.getName())
                .questionDomainType("OrderOperators")
                .questionType(QuestionType.ORDER)
                .statementFacts(facts)
                .answerObjects(bulkMode ? List.of() : answerObjects)
                .options(orderQuestionOptions)
                .build();
    }

    /**
     * Шаг 5: Построение метаданных для вопроса с поиском возможных ошибок и понятий предметной области, формированием тегов
     */
    protected void processMetadata(SupportedLanguage language) {
        SerializableQuestion.QuestionMetadata metadata = source == null ? null : source.getData().getMetadata();
        tags = new ArrayList<>(List.of("basics", "operators", "order", "evaluation", "errors"));
        String languageStr = language.toString();
        tags.add(languageStr.substring(0, 1).toUpperCase() + languageStr.substring(1));

        int solutionLength = answerObjects.size() - 1;
        possibleViolations = findPossibleViolations(tokens);
        concepts = findConcepts(sourceExpressionTree, language);
        double complexity = 0.18549906 * solutionLength - 0.01883239 * possibleViolations.size();
        long conceptBits = concepts.stream().map(domain::getConcept).filter(Objects::nonNull).map(Concept::getBitmask).reduce((a, b) -> a|b).orElse(0L);
        String customTemplateId = rawTranslatedCode.replaceAll("[^a-zA-Z0-9._-]", "");

        this.metadata = SerializableQuestion.QuestionMetadata.builder()
                .name(metadata != null ? metadata.getName() : customTemplateId.concat("_v"))
                .domainShortname(metadata != null ? metadata.getDomainShortname() : "expression")
                .templateId(metadata != null ? metadata.getTemplateId() : customTemplateId)
                .tagBits(tags.stream().map(domain::getTag).filter(Objects::nonNull).map(Tag::getBitmask).reduce((a, b) -> a|b).orElse(0L))
                .conceptBits(conceptBits)
                .lawBits(metadata != null ? metadata.getLawBits() : 1)
                .violationBits(possibleViolations.stream().map(domain::getNegativeLaw).filter(Objects::nonNull).map(Law::getBitmask).reduce((a, b) -> a|b).orElse(0L))
                .traceConceptBits(conceptBits)
                .solutionStructuralComplexity(solutionLength + 1)
                .integralComplexity(complexity)
                .solutionSteps(solutionLength)
                .distinctErrorsCount(possibleViolations.size())
                .version(TARGET_VERSION)
                .structureHash(metadata != null ? metadata.getStructureHash() : "")
                .origin(metadata != null ? (metadata.getOrigin().startsWith("mt_") ? "" :"mt_").concat(metadata.getOrigin()) :
                        (questionOrigin.startsWith("mt_") ? "" :"mt_").concat(questionOrigin))
                .build();
    }

    /**
     * Шаг 4: Построение StatementFacts для шаблона вопроса
     */
    protected void processTemplateStatementFacts() {
        RDFSerializer rdfSerializer = new RDFSerializer();
        Model m = rdfSerializer.serialize(sourceExpressionTree.getRootNode());
        this.stmtFacts = MeaningTreeRDFHelper.backendFactsToSerialized(MeaningTreeRDFHelper.factsFromModel(m));
    }


    /**
     * Поиск понятий предметной области для домена DecisionTree
     * @return множество уникальных понятий предметной области (концептов)
     */
     static Set<String> findConcepts(MeaningTree mt, SupportedLanguage toLanguage) {
        HashSet<String> result = new HashSet<>();
        List<Node> descendants = mt.getRootNode().walkAllNodes();
        for (Node node: descendants) {
            if (node instanceof AddOp) result.add("operator_binary_+");
            else if (node instanceof MulOp) result.add("operator_binary_*");
            else if (node instanceof DivOp) result.add("operator_/");
            else if (node instanceof SubOp) result.add("operator_binary_-");
            else if (node instanceof FloorDivOp) result.add("operator_//");
            else if (node instanceof ModOp) result.add("operator_%");
            else if (node instanceof MatMulOp) result.add("operator_@");
            else if (node instanceof UnaryMinusOp) result.add("operator_unary_-");
            else if (node instanceof UnaryPlusOp) result.add("operator_unary_+");
            else if (node instanceof LtOp) result.add("operator_<");
            else if (node instanceof GtOp) result.add("operator_>");
            else if (node instanceof EqOp) result.add("operator_==");
            else if (node instanceof NotEqOp) result.add("operator_!=");
            else if (node instanceof LeOp) result.add("operator_<=");
            else if (node instanceof GeOp) result.add("operator_>=");
            else if (node instanceof ThreeWayComparisonOp) result.add("operator_<=>");
            else if (node instanceof ContainsOp) result.add("operator_in");
            else if (node instanceof InversionOp) result.add("operator_~");
            else if (node instanceof BitwiseAndOp) result.add("operator_binary_&");
            else if (node instanceof BitwiseOrOp) result.add("operator_|");
            else if (node instanceof XorOp) result.add("operator_^");
            else if (node instanceof LeftShiftOp) result.add("operator_<<");
            else if (node instanceof RightShiftOp) result.add("operator_>>");
            else if (node instanceof IndexExpression) result.add("operator_subscript");
            else if (node instanceof PointerPackOp) result.add("operator_unary_&");
            else if (node instanceof PointerUnpackOp) result.add("operator_unary_*");
            else if (node instanceof PointerMemberAccess) result.add("operator_->");
            else if (node instanceof MemberAccess) result.add("operator_.");
            else if (node instanceof TernaryOperator) result.add("operator_?");
            else if (node instanceof ExpressionSequence && toLanguage == SupportedLanguage.CPP) result.add("operator_,");
            else if (node instanceof FunctionCall) {
                result.add("operator_function_call");
                result.add("function_call");
            }
            else if (node instanceof ReferenceEqOp) {
                if (toLanguage == SupportedLanguage.PYTHON) {
                    result.add("operator_is");
                } else {
                    result.add("operator_==");
                }
            }
            else if (node instanceof ShortCircuitAndOp) {
                if (toLanguage == SupportedLanguage.PYTHON) {
                    result.add("operator_and");
                }
                result.add("operator_&&");
            } else if (node instanceof ShortCircuitOrOp) {
                if (toLanguage == SupportedLanguage.PYTHON) {
                    result.add("operator_or");
                }
                result.add("operator_||");
            }
            else if (node instanceof AssignmentExpression expr) {
                switch (expr.getAugmentedOperator()) {
                    case NONE -> {
                        if (toLanguage == SupportedLanguage.PYTHON) {
                            result.add("operator_:=");
                        } else {
                            result.add("operator_=");
                        }
                    }
                    case ADD -> {
                        result.add("operator_+=");
                    }
                    case SUB -> {
                        result.add("operator_-=");
                    }
                    case MUL -> {
                        result.add("operator_*=");
                    }
                    case DIV -> {
                        result.add("operator_/=");
                    }
                    case FLOOR_DIV -> {
                        result.add("operator_//=");
                    }
                    case BITWISE_AND -> {
                        result.add("operator_&=");
                    }
                    case BITWISE_OR -> {
                        result.add("operator_|=");
                    }
                    case BITWISE_XOR -> {
                        result.add("operator_^=");
                    }
                    case BITWISE_SHIFT_LEFT -> {
                        result.add("operator_<<=");
                    }
                    case BITWISE_SHIFT_RIGHT -> {
                        result.add("operator_>>=");
                    }
                    case MOD -> {
                        result.add("operator_%=");
                    }
                    case POW -> {
                        result.add("operator_**=");
                    }
                }
            }
        }
        return result;
    }

    /**
     * Поиск возможных допускаемых студентом ошибок в выражении вопроса
     * @return набор уникальных возможных ошибок
     */
    static Set<String> findPossibleViolations(TokenList tokens) {
        Set<String> set = new TreeSet<>();
        set.add("error_base_student_error_early_finish");

        OperatorToken lastOperatorToken = null;
        boolean isInComplex = false;
        boolean hasOperatorInComplex = false;
        boolean hasOperatorsOutsideComplex = false;
        for (Token token : tokens) {
            if (List.of(TokenType.OPENING_BRACE, TokenType.CALL_OPENING_BRACE).contains(token.type)) {
                isInComplex = true;
            } else if (List.of(TokenType.CALL_CLOSING_BRACE, TokenType.CLOSING_BRACE).contains(token.type)) {
                isInComplex = false;
            }

            if (token instanceof OperatorToken op) {
                if (
                        lastOperatorToken != null
                                && lastOperatorToken.isStrictOrder
                                && (op.precedence < lastOperatorToken.precedence || isInComplex)
                ) {
                    set.add("error_base_student_error_strict_operands_order");
                    set.add("error_base_student_error_unevaluated_operand");
                }

                if (token.type == TokenType.SUBSCRIPT_OPENING_BRACE || token.type == TokenType.SUBSCRIPT_CLOSING_BRACE) {
                    set.add("error_base_enclosing_operators");
                }

                if (isInComplex) {
                    hasOperatorInComplex = true;
                } else {
                    hasOperatorsOutsideComplex = true;
                }

                if (lastOperatorToken != null && op.precedence > lastOperatorToken.precedence) {
                    set.add("error_base_higher_precedence_right");
                    set.add("precedence");
                } else if (lastOperatorToken != null && lastOperatorToken.precedence > op.precedence) {
                    set.add("error_base_higher_precedence_left");
                    set.add("precedence");
                } else if (lastOperatorToken != null
                        && lastOperatorToken.assoc == op.assoc
                        && lastOperatorToken.assoc == OperatorAssociativity.LEFT
                ) {
                    set.add("error_base_same_precedence_left_associativity_left");
                    set.add("associativity");
                } else if (lastOperatorToken != null
                        && lastOperatorToken.assoc == op.assoc
                        && lastOperatorToken.assoc == OperatorAssociativity.RIGHT
                ) {
                    set.add("error_base_same_precedence_right_associativity_right");
                    String s = null;
                    if (op.arity == OperatorArity.BINARY) {
                        s = "binary";
                    } else if (op.arity == OperatorArity.UNARY) {
                        s = "unary";
                    } else if (op.arity == OperatorArity.TERNARY) {
                        s = "ternary";
                    }

                    if (s != null) {
                        set.add(String.format("error_base_%s_having_associativity_left", s));
                    }

                    set.add("associativity");
                }
                lastOperatorToken = op;
            }
            if (hasOperatorInComplex && hasOperatorsOutsideComplex) {
                set.add("error_base_student_error_in_complex");
            }
        }

        return set;
    }

    static String questionToHtml(TokenList tokens,
                                 ProgrammingLanguageExpressionDTDomain domain,
                                 Language lang
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p class='comp-ph-expr'>");
        int idx = 0;
        int answerIdx = -1;
        for (Token t: tokens) {
            String tokenValue = t.value;
            if (t instanceof OperatorToken) {
                sb.append("<span data-comp-ph-pos='").append(++idx).append("' id='answer_").append(++answerIdx).append("' class='comp-ph-expr-op-btn'").append(">").append(tokenValue).append("</span>");
            } else {
                sb.append("<span data-comp-ph-pos='").append(++idx).append("' class='comp-ph-expr-const'").append(">").append(tokenValue).append("</span>");
            }
        }

        sb.append("<br/><button data-comp-ph-pos='").append(++idx).append("' id='answer_").append(++answerIdx).append("' class='btn comp-ph-complete-btn' data-comp-ph-value=''>").append(
                lang != null && domain != null ? domain.getMessage("student_end_evaluation", lang) : "everything is evaluated"
        ).append("</button>");

        sb.append("<!-- Original expression: ");
        sb.append(tokens.stream().map((Token t) -> t.value).collect(Collectors.joining(" ")));
        sb.append(' ');
        sb.append("-->").append("</p>");
        String text = sb.toString();

        sb = new StringBuilder(text
                .replaceAll("\\*", "&#8727")
                .replaceAll("\\n", "<br>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;"));
        sb.insert(0, "<div class='comp-ph-question'>"); sb.append("</div>");
        return sb.toString();
    }
}
