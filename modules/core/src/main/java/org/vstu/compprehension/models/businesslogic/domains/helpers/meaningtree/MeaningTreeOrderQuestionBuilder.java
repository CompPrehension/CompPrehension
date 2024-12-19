package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.vstu.compprehension.common.MathHelper;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestionTemplate;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
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
import org.vstu.meaningtree.serializers.rdf.RDFSerializer;
import org.vstu.meaningtree.utils.tokens.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;


@Log4j2
public class MeaningTreeOrderQuestionBuilder {
    /***
     * Построитель сериализованного вопроса SerializableQuestion, пригодного для сохранения в хранилище
     * Выражение может быть построено на любом поддерживаемом языке программирования
     * и преобразовано в другой поддерживаемый язык программирования с помощью использования общего дерева языков проекта MeaningTree
     *
     * Представляет собой прослойку, пригодную для генерирования и выдачи уже существующих вопросов на любом поддерживаемом языке программирования
     */

    protected MeaningTree sourceExpressionTree = null; // выражение
    protected QuestionMetadataEntity existingMetadata = null;

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
    protected SerializableQuestionTemplate.QuestionMetadata metadata;
    protected SerializableQuestion.QuestionData qdata;
    protected List<String> tags;
    protected Set<String> concepts;
    protected Set<String> possibleViolations;

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
     * @param q объект вопроса
     * @param domain поддерживаемый домен
     * @return построитель вопроса
     */
    public static MeaningTreeOrderQuestionBuilder fromExistingQuestion(Question q, ProgrammingLanguageExpressionDTDomain domain) {
        MeaningTree mt;
        if (q.getMetadata().getVersion() >= MIN_VERSION) {
            mt = MeaningTreeRDFHelper.backendFactsToMeaningTree(
                    q.getQuestionData().getStatementFacts()
            );
        } else {
            log.info("Converting old-format question with metadata id={}", q.getMetadata().getId());
            mt = extractExpression(q.getQuestionData().getStatementFacts());
        }
        MeaningTreeOrderQuestionBuilder builder = new MeaningTreeOrderQuestionBuilder(domain);
        builder.sourceExpressionTree = mt;
        builder.existingMetadata = q.getMetadata();
        builder.questionOrigin(q.getMetadata().getOrigin());
        return builder;
    }

    // Извлечение выражения из существующего вопроса в старом формате и превращение его в общее дерево
    protected static MeaningTree extractExpression(List<BackendFactEntity> facts) {
        CppTranslator cppTranslator = new CppTranslator(new MeaningTreeDefaultExpressionConfig());
        StringBuilder tokenBuilder = new StringBuilder();

        Map<Integer, String> indexes = new HashMap<>();
        Map<String, String> tokenValues = new HashMap<>();
        Map<String, Boolean> semanticValues = new HashMap<>();

        for (BackendFactEntity st : facts) {
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
        log.info("Extracted expression text from existing question: {}", tokens);
        TokenList tokenList = cppTranslator.getTokenizer().tokenizeExtended(tokens);
        HashMap<TokenGroup, Object> semanticValuesIndexes = new HashMap<>();
        for (int i = 0; i < indexes.keySet().stream().max(Long::compare).orElse(0); i++) {
            if (semanticValues.containsKey(indexes.get(i))) {
                semanticValuesIndexes.put(new TokenGroup(i, i + 1, tokenList), semanticValues.get(indexes.get(i)));
            }
        }
        return cppTranslator.getMeaningTree(tokenList, semanticValuesIndexes);
    }


    // Определить по тегам язык программирования
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

    /**
     * Создать новый вопрос из выражения
     * @param expression строковое представление выражения
     * @param language язык программирования
     * @param domain домен, для которого создаем вопрос
     * @return построитель вопроса
     */
    public static MeaningTreeOrderQuestionBuilder newQuestion(String expression,
                                                              SupportedLanguage language,
                                                              ProgrammingLanguageExpressionDTDomain domain) {
        MeaningTreeOrderQuestionBuilder builder = new MeaningTreeOrderQuestionBuilder(domain);
        try {
            builder.sourceExpressionTree = language.createTranslator(new MeaningTreeDefaultExpressionConfig()).getMeaningTree(expression);
            return builder;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Cannot create source translator with ".concat(language.toString()));
        }

    }

    /**
     * Отдельная генерация объектов ответа под заданные токены. Обычно вызывается перед выдачей самого вопроса
     * @param tokens токены
     * @return список объектов ответа
     */
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

    /**
     * Построить новый вопрос из общего дерева под заданный язык
     * @param mt общее дерево
     * @param language заданный язык программирования
     * @param domain домен, под который будет осуществляться генерация
     * @return построитель вопроса
     */
    public static MeaningTreeOrderQuestionBuilder newQuestion(MeaningTree mt, SupportedLanguage language, ProgrammingLanguageExpressionDTDomain domain) {
        MeaningTreeOrderQuestionBuilder builder = new MeaningTreeOrderQuestionBuilder(domain);
        builder.sourceExpressionTree = mt;
        return builder;
    }

    /**
     * Построить один вопрос из уже существующего без возвращения построителя вопроса
     * @param data сущность вопроса из приложения
     * @param lang язык программирования
     * @param domain домен, под который будет осуществляться генерация
     * @return сгенерированный вопрос
     */
    public static Question fastBuildFromExisting(Question data, SupportedLanguage lang, ProgrammingLanguageExpressionDTDomain domain) {
        return MeaningTreeOrderQuestionBuilder.fromExistingQuestion(data, domain)
                .buildQuestion(lang).getFirst();
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
    public List<Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata>> build(SupportedLanguage language) {
        answerObjects = new ArrayList<>();
        processTemplateStatementFacts();
        return generateManyQuestions(language);
    }

    /**
     * Построить вопрос для всех поддерживаемых языков
     * @return список вопросов для каждого языка в формате: один общий вопрос, содержащий общее дерево для всех языков и несколько метаданных для разных языков
     */
    public List<SerializableQuestionTemplate> buildAll() {
        List<SerializableQuestionTemplate> resultList = new ArrayList<>();

        List<Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata>> metadata = new ArrayList<>();
        for (SupportedLanguage language : SupportedLanguage.getMap().keySet()) {
            metadata.addAll(build(language));
        }

        for (List<Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata>> grouped
            : metadata.stream().collect(
                    Collectors.groupingBy(pair ->
                            pair.getRight().getTreeHashCode())).values()
        ) {
            SerializableQuestion common = grouped.getFirst().getLeft();
            List<SerializableQuestionTemplate.QuestionMetadata> metadataList = grouped.stream().map(Pair::getRight).toList();

            resultList.add(SerializableQuestionTemplate.builder().metadataList(metadataList).commonQuestion(common).build());
        }
        return resultList;
    }

    /**
     * Построить объекты вопроса приложения по заданному построителю вопроса
     * @param lang язык, под который нужно сгенерировать вопрос
     * @return
     */
    public List<Question> buildQuestion(SupportedLanguage lang) {
        return build(lang).stream().map(
                (Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata> q) ->
                        q.getKey().toQuestion(domain, q.getValue().toMetadataEntity())).toList();
    }

    private String debugTokensString(MeaningTree mt, SupportedLanguage lang) {
        try {
            TokenList list = lang.createTranslator(new MeaningTreeDefaultExpressionConfig()).getTokenizer().tokenizeExtended(mt);

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                builder.append(list.get(i).value);
                if (list.get(i).getAssignedValue() != null) {
                    builder.append("<--");
                    builder.append(list.get(i).getAssignedValue().toString().toUpperCase());
                    builder.append(';');
                }
                builder.append(' ');
            }
            return builder.toString();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException(e);
        }
    }

    /**
     * Формирование уникального вопроса из шаблона и значений токенов
     * @return сериализуемый вопрос
     */
    protected Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata> generateFromTemplate(SupportedLanguage lang, MeaningTree mt, int treeHash) {
        Model model = new RDFSerializer().serialize(mt.getRootNode());
        List<SerializableQuestion.StatementFact> facts = MeaningTreeRDFHelper.backendFactsToSerialized(
                MeaningTreeRDFHelper.factsFromModel(model));
        processMetadata(lang, treeHash);
        processQuestionData(facts);
        SerializableQuestion serialized = SerializableQuestion.builder()
                .questionData(qdata)
                .concepts(List.of())
                .negativeLaws(List.of())
                .tags(defaultTags)
                .build();
        log.info("Created question: {}", debugTokensString(mt, lang));
        return new ImmutablePair<>(serialized, metadata);
    }

    /**
     * Формирование уникального вопроса из шаблона без дополнительных значений
     * @return сериализуемый вопрос
     */
    protected Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata> generateFromTemplate(SupportedLanguage lang) {
        return generateFromTemplate(lang, sourceExpressionTree, sourceExpressionTree.hashCode());
    }

    /**
     * Генерирование нескольких вопросов из одного шаблона
     * @return много сериализуемых вопросов из одного шаблона
     */
    protected List<Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata>> generateManyQuestions(SupportedLanguage language) {
        processTokens(language);
        answerObjects = generateAnswerObjects(tokens);

        if (tokens.stream().anyMatch((Token t) -> t.getAssignedValue() != null)) {
            log.debug("Given data already contains values paired with tokens");
            return List.of(generateFromTemplate(language));
        }
        List<Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata>> generated = new ArrayList<>();
        OperandEvaluationMap map = new OperandEvaluationMap(this, language);
        List<Pair<MeaningTree, Integer>> generatedValues = map.generate();
        MeaningTree initial = sourceExpressionTree;
        for (Pair<MeaningTree, Integer> pair : generatedValues) {
            sourceExpressionTree = pair.getLeft();
            generated.add(generateFromTemplate(language, pair.getLeft(), pair.getRight()));
        }
        if (generatedValues.isEmpty()) {
            return List.of(generateFromTemplate(language));
        }
        sourceExpressionTree = initial;

        return generated;
    }

    /**
     * Создание токенов выражения из результирующего общего дерева (т.е. для выходных данных)
     */
    protected void processTokens(SupportedLanguage language) {
        LanguageTranslator toTranslator;
        try {
            toTranslator = language.createTranslator(new MeaningTreeDefaultExpressionConfig());
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Cannot create source translator with ".concat(language.toString()));
        }
        rawTranslatedCode = toTranslator.getCode(sourceExpressionTree);
        tokens = toTranslator.getTokenizer().tokenizeExtended(rawTranslatedCode);
    }

    /**
     * Формирование данных уникального вопроса
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
                .answerObjects(List.of())
                .options(orderQuestionOptions)
                .build();
    }

    /**
     * Построение метаданных для вопроса с поиском возможных ошибок и понятий предметной области, формированием тегов
     */
    protected void processMetadata(SupportedLanguage language, int treeHash) {
        QuestionMetadataEntity metadata = existingMetadata == null ? null : existingMetadata;
        tags = new ArrayList<>(List.of("basics", "operators", "order", "evaluation", "errors"));
        String languageStr = language.toString();
        tags.add(languageStr.substring(0, 1).toUpperCase() + languageStr.substring(1));

        int omitted = findOmitted(sourceExpressionTree);
        int solutionLength = answerObjects.size() - omitted;

        possibleViolations = findPossibleViolations(tokens);
        Set<String> possibleSkills = findSkills(tokens);
        concepts = findConcepts(sourceExpressionTree, language);
        double complexity = 0.18549906 * solutionLength - 0.01883239 * possibleViolations.size();
        complexity = MathHelper.sigmoid(complexity * 4 - 2);
        long conceptBits = concepts.stream().map(domain::getConcept).filter(Objects::nonNull).map(Concept::getBitmask).reduce((a, b) -> a|b).orElse(0L);

        String customTemplateId = rawTranslatedCode.replaceAll(" ", "_").replaceAll("[/:*?\"<>|\\\\]", "");
        String customQuestionId = customTemplateId.concat(Integer.toString(treeHash)).concat("_v");

        this.metadata = SerializableQuestionTemplate.QuestionMetadata.builder()
                .name(metadata != null ? metadata.getName() : customQuestionId)
                .domainShortname(metadata != null ? metadata.getDomainShortname() : "expression")
                .templateId(metadata != null ? metadata.getTemplateId() : customTemplateId)
                .tagBits(tags.stream().map(domain::getTag).filter(Objects::nonNull).map(Tag::getBitmask).reduce((a, b) -> a|b).orElse(0L))
                .conceptBits(conceptBits)
                .skillBits(possibleSkills.stream().map(domain::getSkill).filter(Objects::nonNull).map(Skill::getBitmask).reduce((a, b) -> a|b).orElse(0L))
                .lawBits(metadata != null ? metadata.getLawBits() : 1)
                .violationBits(possibleViolations.stream().map(domain::getNegativeLaw).filter(Objects::nonNull).map(Law::getBitmask).reduce((a, b) -> a|b).orElse(0L))
                .traceConceptBits(conceptBits)
                .solutionStructuralComplexity(solutionLength + 1)
                .integralComplexity(complexity)
                .solutionSteps(solutionLength)
                .distinctErrorsCount(possibleViolations.size())
                .version(TARGET_VERSION)
                .treeHashCode(treeHash)
                .language(language.toString())
                .structureHash(metadata != null ? metadata.getStructureHash() : "")
                .origin(metadata != null ? (metadata.getOrigin().startsWith("mt_") ? "" :"mt_").concat(metadata.getOrigin()) :
                        (questionOrigin.startsWith("mt_") ? "" :"mt_").concat(questionOrigin))
                .build();
    }

    /**
     * Найти в сгенерированном дереве вопроса опущенные для вычисления операнды, сокращающие число шагов
     * @param tree - заданное дерево
     * @return число опущенных операндов
     */
    static int findOmitted(MeaningTree tree) {
        int count = 0;
        for (Node.Info info : tree) {
            if (info.node() instanceof ShortCircuitAndOp
                    && info.node().getAssignedValueTag() instanceof Boolean bool && !bool) {
                count++;
            } else if (info.node() instanceof ShortCircuitOrOp
                    && info.node().getAssignedValueTag() instanceof Boolean bool && bool) {
                count++;
            } else if (info.node() instanceof TernaryOperator) {
                count++;
            }
        }
        return count;
    }

    /**
     * Построение StatementFacts для шаблона вопроса
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

    /**
     * Поиск возможных навыков, которые может получить студент
     * @return набор уникальных возможных навыков
     */
    static Set<String> findSkills(TokenList tokens) {
        Set<String> set = new TreeSet<>();
        set.add("expression_fully_evaluated");
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t instanceof OperandToken op && op.operandPosition() == OperandPosition.CENTER) {
                set.add("central_operand_needed");
                if (op instanceof OperatorToken) {
                    set.add("is_central_operand_evaluated");
                }
            }

            if (t instanceof OperandToken op && (op.operandPosition() == OperandPosition.LEFT || op.operandPosition() == OperandPosition.RIGHT)) {
                int incr = op.operandPosition() == OperandPosition.LEFT ? -1 : 1;
                set.add("nearest_operand_needed");
                OperatorToken foundNearestOp = null;
                for (int j = i;
                     op.operandPosition() == OperandPosition.LEFT ? j >= 0 : j < tokens.size();
                     j += incr) {
                    if (tokens.get(j) instanceof OperatorToken leftOp) {
                        set.add("competing_operator_present");
                        foundNearestOp = leftOp;
                        break;
                    }
                }

                if (foundNearestOp instanceof ComplexOperatorToken complex && complex.isOpening() && complex.equals(tokens.isInComplex(i))) {
                    set.add("current_operator_enclosed");
                }

                if (foundNearestOp != null && tokens.isParenthesized(tokens.indexOf(foundNearestOp)) && !tokens.isParenthesized(i)) {
                    set.add("is_nearest_parenthesized_current_not");
                } else if (foundNearestOp != null && !tokens.isParenthesized(tokens.indexOf(foundNearestOp)) && tokens.isParenthesized(i)) {
                    set.add("is_current_parenthesized_nearest_not");
                }

                if (foundNearestOp != null && foundNearestOp.precedence != op.operandOf().precedence) {
                    set.add("order_determined_by_precedence");
                } else {
                    if (op.operandOf().arity == OperatorArity.UNARY) {
                        set.add("associativity_without_opposing_operand");
                    } else {
                        set.add("order_determined_by_associativity");
                    }
                }
            }
            if (t instanceof OperatorToken op && op.isStrictOrder) {
                set.add("strict_order_operators_present");
                set.add("is_current_strict_order");
                Map<OperandPosition, TokenGroup> ops = tokens.findOperands(i);
                set.add("strict_order_first_operand_to_be_evaluated");
                if (ops.containsKey(op.getFirstOperandToEvaluation()) && ops.get(op.getFirstOperandToEvaluation())
                        .asSublist()
                        .stream()
                        .anyMatch((Token tt) -> tt instanceof OperatorToken)) {
                    set.add("is_first_operand_of_strict_order_operator_fully_evaluated");
                }
                if (op instanceof ComplexOperatorToken) {
                    set.add("no_omitted_operands_despite_strict_order");
                    set.add("should_strict_order_current_operand_be_omitted");
                }
                if (op.type == TokenType.CALL_OPENING_BRACE || op.type == TokenType.CALL_CLOSING_BRACE) {
                    set.add("are_central_operands_strict_order");
                    set.add("no_comma_in_central_operands");
                    if (ops.containsKey(OperandPosition.CENTER) && ops.get(OperandPosition.CENTER).length() > 1) {
                        set.add("no_current_in_many_central_operands");
                        set.add("previous_central_operands_are_unevaluated");
                    }
                }
            }
        }
        return set;
    }

    /**
     * Создание текста вопроса, рекомендуемое для вызова перед выдачей вопроса
     * @param tokens токены результирующего вопроса
     * @param domain домен, для которого генерируется
     * @param lang язык программирования
     * @return строка вопроса в HTML
     */
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
