package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.vstu.compprehension.common.MathHelper;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.common.Utils;
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
import org.vstu.meaningtree.nodes.expressions.ParenthesizedExpression;
import org.vstu.meaningtree.nodes.expressions.bitwise.*;
import org.vstu.meaningtree.nodes.expressions.calls.FunctionCall;
import org.vstu.meaningtree.nodes.expressions.comparison.*;
import org.vstu.meaningtree.nodes.expressions.literals.PlainCollectionLiteral;
import org.vstu.meaningtree.nodes.expressions.logical.NotOp;
import org.vstu.meaningtree.nodes.expressions.logical.ShortCircuitAndOp;
import org.vstu.meaningtree.nodes.expressions.logical.ShortCircuitOrOp;
import org.vstu.meaningtree.nodes.expressions.math.*;
import org.vstu.meaningtree.nodes.expressions.newexpr.NewExpression;
import org.vstu.meaningtree.nodes.expressions.other.*;
import org.vstu.meaningtree.nodes.expressions.pointers.PointerMemberAccess;
import org.vstu.meaningtree.nodes.expressions.pointers.PointerPackOp;
import org.vstu.meaningtree.nodes.expressions.pointers.PointerUnpackOp;
import org.vstu.meaningtree.nodes.expressions.unary.*;
import org.vstu.meaningtree.nodes.io.InputCommand;
import org.vstu.meaningtree.nodes.io.PrintCommand;
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
    protected String questionOrigin = null; // источник вопроса для метаданных
    protected String originLicense = null; // лицензия источника для метаданных

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

    private boolean allChecksArePassed = true;

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
        builder.questionOrigin(q.getMetadata().getOrigin(), q.getMetadata().getOriginLicense());
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

    // Определить по тегам язык программирования
    public static SupportedLanguage detectLanguageFromTags(long tags, ProgrammingLanguageExpressionDTDomain domain) {
        // Считаем, что в тегах может быть указан только один язык
        List<String> languages = SupportedLanguage.getMap().keySet().stream().map(SupportedLanguage::toString).toList();
        for (Tag tag : domain.tagsFromBitmask(tags)) {
            if (languages.contains(tag.getName().toLowerCase())) {
                return SupportedLanguage.fromString(tag.getName().toLowerCase());
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

    public static QuestionMetadataEntity metadataRecalculate(ProgrammingLanguageExpressionDTDomain domain,
                                                                                    QuestionMetadataEntity qMeta) {
        Question q = qMeta.getQuestionData().getData().toQuestion(domain, qMeta);
        MeaningTreeOrderQuestionBuilder builder = MeaningTreeOrderQuestionBuilder.fromExistingQuestion(q, domain);
        SupportedLanguage language = detectLanguageFromTags(qMeta.getTagBits(), domain);
        builder.processTokensDebug(language);
        if (!builder.allChecksArePassed) {
            return null;
        }
        builder.answerObjects = generateAnswerObjects(builder.tokens);
        builder.processMetadata(language, builder.sourceExpressionTree.hashCode());
        if (!builder.allChecksArePassed) {
            return null;
        }
        return builder.metadata.toMetadataEntity();
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
                if (t instanceof ComplexOperatorToken complex && complex.isClosing()) {
                    tokenId++;
                    continue;
                }
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
    public MeaningTreeOrderQuestionBuilder questionOrigin(String name, String license) {
        questionOrigin = name;
        originLicense = license;
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

        return !allChecksArePassed ? List.of() : generated;
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
        var result = toTranslator.tryGetCode(sourceExpressionTree);
        allChecksArePassed &= result.getLeft();
        rawTranslatedCode = result.getRight();
        if (rawTranslatedCode != null) {
            var tokenRes = toTranslator.getTokenizer().tryTokenizeExtended(sourceExpressionTree);
            allChecksArePassed &= tokenRes.getLeft();
            tokens = tokenRes.getLeft() ? tokenRes.getRight() : new TokenList();
        } else {
            tokens = new TokenList();
        }
    }

    /**
     * Для целей отладки и переклассификации, точная проверка несовместимости преобразований
     */
    private void processTokensDebug(SupportedLanguage language) {
        LanguageTranslator toTranslator;
        try {
            toTranslator = language.createTranslator(new MeaningTreeDefaultExpressionConfig());
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Cannot create source translator with ".concat(language.toString()));
        }
        var result = toTranslator.tryGetCode(sourceExpressionTree);
        allChecksArePassed &= result.getLeft();
        rawTranslatedCode = result.getRight();
        if (rawTranslatedCode != null) {
            var tokenRes = toTranslator.getTokenizer().tryTokenizeExtended(sourceExpressionTree);
            allChecksArePassed &= tokenRes.getLeft();
            if (tokenRes.getLeft()) {
                var tokenRes2 = toTranslator.getTokenizer().tryTokenizeExtended(rawTranslatedCode);
                allChecksArePassed &= tokenRes2.getLeft();
            }
            tokens = tokenRes.getLeft() ? tokenRes.getRight() : new TokenList();
        } else {
            tokens = new TokenList();
        }
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
        if (questionOrigin == null || questionOrigin.isEmpty()) {
            throw new MeaningTreeException("Question origin didn't specified");
        }

        int omitted = findOmitted(sourceExpressionTree);
        int solutionLength = answerObjects.size() - omitted;
        if (solutionLength <= 0) {
            solutionLength = 1;
        }

        var violations = findAllPossibleViolations(tokens);
        var skills = findAllSkills(tokens, language);

        // Отсеиваем вопросы, в которых много шагов и при этом больше есть повторяющиеся скиллы и ошибки
        final int targetSolutionLength = 16;
        final int maxSkillRepeatCount = 8;
        final int maxErrorRepeatCount = 5;
        if (solutionLength > targetSolutionLength) {
            var counter = Utils.countElements(skills);
            for (var entry : counter.entrySet()) {
                if (entry.getValue() > maxSkillRepeatCount) {
                    allChecksArePassed = false;
                }
            }
            counter = Utils.countElements(violations);
            for (var entry : counter.entrySet()) {
                if (entry.getValue() > maxErrorRepeatCount) {
                    allChecksArePassed = false;
                }
            }
        }

        possibleViolations = new HashSet<>(violations);
        Set<String> possibleSkills = new HashSet<>(skills);
        concepts = findConcepts(sourceExpressionTree, language);
        if (concepts.isEmpty() || solutionLength == 1) {
            allChecksArePassed = false;
        }
        double complexity = 0.18549906 * solutionLength - 0.01883239 * possibleViolations.size();
        complexity = MathHelper.sigmoid(complexity * 4 - 2);
        long conceptBits = concepts.stream().map(domain::getConcept).filter(Objects::nonNull).map(Concept::getBitmask).reduce((a, b) -> a|b).orElse(0L);

        String customTemplateId = StringHelper.truncate(rawTranslatedCode.replaceAll(
                " ", "_").replaceAll("[/:*?\"<>|\\\\]", ""),
                64).concat("_").concat(languageStr);
        String customQuestionId = customTemplateId.concat(Integer.toString(treeHash)).concat("_v");

        this.metadata = SerializableQuestionTemplate.QuestionMetadata.builder()
                .name(customQuestionId)
                .domainShortname(domain.getShortnameForQuestionSearch())
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
                .origin(questionOrigin)
                .originLicense(originLicense)
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
            if (info.node() instanceof ShortCircuitAndOp op
                    && op.getLeft().getAssignedValueTag() instanceof Boolean bool && !bool) {
                count++;
            } else if (info.node() instanceof ShortCircuitOrOp op
                    && op.getLeft().getAssignedValueTag() instanceof Boolean bool && bool) {
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
        for (Node.Info nodeInfo: mt) {
            Node node = nodeInfo.node();
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
            else if (node instanceof SizeofExpression) result.add("operator_sizeof");
            else if (node instanceof NewExpression) result.add("operator_new");
            else if (node instanceof PlainCollectionLiteral) result.add("collection_literal");
            else if (node instanceof CastTypeExpression) result.add("operator_cast");
            else if (node instanceof InversionOp) result.add("operator_~");
            else if (node instanceof BitwiseAndOp) result.add("operator_binary_&");
            else if (node instanceof BitwiseOrOp) result.add("operator_|");
            else if (node instanceof XorOp) result.add("operator_^");
            else if (node instanceof LeftShiftOp) result.add("operator_<<");
            else if (node instanceof ParenthesizedExpression) result.add("operator_(");
            else if (node instanceof PrintCommand || node instanceof InputCommand) result.add("stream_io");
            else if (node instanceof RightShiftOp) result.add("operator_>>");
            else if (node instanceof IndexExpression) result.add("operator_subscript");
            else if (node instanceof PointerPackOp) result.add("operator_unary_&");
            else if (node instanceof PointerUnpackOp) result.add("operator_unary_*");
            else if (node instanceof PointerMemberAccess) result.add("operator_->");
            else if (node instanceof PrefixIncrementOp) result.add("operator_prefix_++");
            else if (node instanceof PrefixDecrementOp) result.add("operator_prefix_--");
            else if (node instanceof PostfixIncrementOp) result.add("operator_postfix_++");
            else if (node instanceof PostfixDecrementOp) result.add("operator_postfix_--");
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
            } else if (node instanceof NotOp) {
                 result.add("operator_!");
            }
            else if (node instanceof AssignmentExpression expr) {
                switch (expr.getAugmentedOperator()) {
                    case NONE -> {
                        if (toLanguage == SupportedLanguage.PYTHON) {
                            result.add("operator_:=");
                        }
                        result.add("operator_=");
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
    public static Set<String> findPossibleViolations(TokenList tokens) {
        return new HashSet<>(findAllPossibleViolations(tokens));
    }

    /**
     * Поиск возможных допускаемых студентом ошибок в выражении вопроса
     * @return набор уникальных возможных ошибок
     */
    public static List<String> findAllPossibleViolations(TokenList tokens) {
        List<String> set = new ArrayList<>();
        HashSet<Pair<Integer, Integer>> parentheses = new HashSet<>();
        set.add("error_base_student_error_early_finish");

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token instanceof OperatorToken op) {
                boolean hasDifferentPrec = false; // встречались операторы с разными приоритетами в направлении

                // Посмотрим, в каких скобках данный оператор. Если ни в каких - тоже сохранить это как пару -1, -1
                var currentParen = tokens.getEnclosingParentheses(i);
                parentheses.add(currentParen);

                // Поиск ближайших операторов справа
                for (int j = i; j < tokens.size(); j++) {
                    // Запятая или другой разделитель останавливают поиск ближайшего оператора
                    if (tokens.get(j).type == TokenType.SEPARATOR || (tokens.get(j).type == TokenType.COMMA
                            && !(tokens.get(j) instanceof OperatorToken))) {
                        break;
                    }
                    if (tokens.get(j) instanceof OperatorToken nearOp && i != j) {
                        var paren = tokens.getEnclosingParentheses(j);
                        // Оператор в скобках, которые между двумя операторами
                        if (paren.getLeft() != -1 && i < paren.getLeft() && paren.getLeft() < j) {
                            continue;
                        }

                        // Оператор не должен содержаться в другом
                        if (op.isInOperandOf(nearOp, OperandPosition.CENTER) || nearOp.isInOperandOf(op, OperandPosition.CENTER)) {
                            continue;
                        }

                        boolean higherRightPrecedence = nearOp.precedence > op.precedence;
                        if (higherRightPrecedence && !hasDifferentPrec) {
                            set.add("error_base_higher_precedence_right");
                            set.add("precedence");
                            hasDifferentPrec = true;
                            // Это не останавливает поиск, так как еще может быть оператор с различной ассоциативностью
                        }

                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.LEFT) {
                            set.add("associativity");
                            set.add("error_base_same_precedence_right_associativity_right");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.RIGHT) {
                            set.add("associativity");
                            set.add("error_base_same_precedence_left_associativity_left");
                            break;
                        }
                    }
                }

                // Поиск ближайших операторов слева
                for (int j = i; j >= 0; j--) {
                    // Запятая или другой разделитель останавливают поиск ближайшего
                    if (tokens.get(j).type == TokenType.SEPARATOR || (tokens.get(j).type == TokenType.COMMA
                            && !(tokens.get(j) instanceof OperatorToken))) {
                        break;
                    }
                    if (tokens.get(j) instanceof OperatorToken nearOp && i != j) {
                        var paren = tokens.getEnclosingParentheses(j);
                        // Оператор в скобках, которые между двумя операторами
                        if (paren.getLeft() != -1 && j < paren.getRight() && paren.getRight() < i) {
                            continue;
                        }

                        // Оператор не должен содержаться в другом
                        if (op.isInOperandOf(nearOp, OperandPosition.CENTER) || nearOp.isInOperandOf(op, OperandPosition.CENTER)) {
                            continue;
                        }

                        boolean higherLeftPrecedence = nearOp.precedence > op.precedence;
                        if (higherLeftPrecedence && !hasDifferentPrec) {
                            set.add("error_base_higher_precedence_left");
                            set.add("precedence");
                            hasDifferentPrec = true;
                            // Это не останавливает поиск, так как еще может быть оператор с различной ассоциативностью
                        }

                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.LEFT) {
                            set.add("associativity");
                            set.add("error_base_same_precedence_right_associativity_right");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.RIGHT) {
                            set.add("associativity");
                            set.add("error_base_same_precedence_left_associativity_left");
                            break;
                        }
                    }
                }

                if (op.arity == OperatorArity.UNARY && op.assoc == OperatorAssociativity.LEFT) {
                    set.add("error_base_unary_having_associativity_left");
                }

                if (op.arity == OperatorArity.BINARY && op.assoc == OperatorAssociativity.LEFT) {
                    set.add("error_base_binary_having_associativity_left");
                }

                if (op.arity == OperatorArity.BINARY && op.assoc == OperatorAssociativity.RIGHT) {
                    set.add("error_base_binary_having_associativity_right");
                }

                if (op.operandOf() != null
                        && op.operandOf().type != TokenType.COMMA
                        && op.operandPosition() != op.operandOf().getFirstOperandToEvaluation()
                ) {
                    set.add("error_base_student_error_unevaluated_operand");
                }

                // все операторы строгого порядка
                if (op.operandOf() != null && op.operandOf().isStrictOrder) {
                    set.add("error_base_student_error_strict_operands_order");
                }

                // оператор является чьим-то центральным операндом
                if (op.operandPosition() == OperandPosition.CENTER) {
                    set.add("error_base_enclosing_operators");
                }

                if (op.operandOf() != null && op.operandOf() instanceof ComplexOperatorToken) {
                    set.add("error_base_student_error_in_complex");
                }
            }
        }

        // Есть хотя бы один оператор в скобках, все выражение в скобках (и нигде больше скобок внутри) не считается
        if (parentheses.size() > 1) {
            set.add("error_base_parenthesized_operators");
            set.add("error_base_student_error_in_complex");
        }

        return set;
    }

    /**
     * Поиск возможных навыков, которые может получить студент
     * @return набор уникальных возможных навыков
     */
    public static Set<String> findSkills(TokenList tokens, SupportedLanguage lang) {
        return new TreeSet<>(findAllSkills(tokens, lang));
    }

    /***
     * Определить токен-операнд из двух переданных, заключенный в обычных скобках (не скобках - частей оператора)
     * @param op1 - первый токен, не являющийся скобкой
     * @param op2 - второй токен, не являющийся скобкой
     * @param tokens - все токены выражения
     * @return индекс заключенного в скобки, либо Integer.MAX_VALUE - если оба в разных скобках, -1 - ни один не в скобках
     */
    private static int getParenthesizedOperand(int op1, int op2, TokenList tokens) {
        /**
         * X - первый токен-операнд, Y - второй токен-операнд. Нужно определить какой из них заключен в скобки
         * (X) -> Y или X <- (Y)
         * 1. Смотрим точки в диапазоне [X, Y]
         * 2. Считаем глубины скобок на краях (начальный край равен 0)
         * 3. Считаем по всему маршруту минимальная глубину вложенности
         * 4. Если глубины на краях - разные. В скобках то - у кого глубина больше
         * 5. Если они равны - если минимальная глубина == 0, то ничего не в скобках
         *                   - если минимальная глубина < 0, то каждый в своих скобках
         */
        int depth = 0; // глубина на левом крае диапазона
        int minDepth = 0; // минимальная глубина вложенности на диапазоне
        for (int i = Math.min(op1, op2); i < Math.max(op1, op2); i++) {
            if (tokens.get(i).type.equals(TokenType.OPENING_BRACE)) {
                depth++;
            } else if (tokens.get(i).type.equals(TokenType.CLOSING_BRACE)) {
                depth--;
            }
            minDepth = Math.min(depth, minDepth);
        }
        if (depth > 0) {
            return Math.max(op1, op2);
        } else if (depth < 0) {
            return Math.min(op1, op2);
        } else {
            if (minDepth == 0) {
                return -1; // ни один не в скобах
            } else {
                return Integer.MAX_VALUE; // оба в разных скобках
            }
        }
    }

    /**
     * Поиск возможных навыков, которые может получить студент
     * @return набор уникальных возможных навыков
     */
    public static List<String> findAllSkills(TokenList tokens, SupportedLanguage lang) {
        List<String> set = new ArrayList<>();

        // Хотя бы один оператор, не являющийся комплексным (больше 1 токена)
        if (tokens.stream().anyMatch(t -> t instanceof OperatorToken && !(t instanceof ComplexOperatorToken))) {
            set.add("central_operand_needed");
        }

        // Ни одной операции строгого порядка
        if (tokens.stream().noneMatch(t -> t instanceof OperatorToken op && op.isStrictOrder)) {
            set.add("expression_strict_order_operators_present");
        }

        // Язык c++ не имеет строгого порядка вычисления вызовов функции
        if (lang.equals(SupportedLanguage.CPP)) {
            set.add("are_central_operands_strict_order");
        }

        // Самый левый токен - не префиксный оператор, а самый правый - не постфиксный
        boolean isLeftmostWithNoLeftOperands = tokens.getFirst() instanceof OperatorToken
                && !tokens.findOperands(0).containsKey(OperandPosition.LEFT);
        boolean isRightmostWithNoRightOperands = tokens.getLast() instanceof OperatorToken
                && !tokens.findOperands(tokens.size() - 1).containsKey(OperandPosition.RIGHT);
        if (!(isLeftmostWithNoLeftOperands && isRightmostWithNoRightOperands)) {
            set.add("competing_operator_present");
        }

        HashSet<Pair<Integer, Integer>> parentheses = new HashSet<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);

            // Токен - оператор
            if (t instanceof OperatorToken op) {
                // оператор является чьим-то центральным операндом
                if (op.operandPosition() == OperandPosition.CENTER) {
                    set.add("current_operator_enclosed");
                }

                var operands = tokens.findOperands(i);

                // оператор имеет любой центральный операнд
                if (operands.containsKey(OperandPosition.CENTER)) {
                    set.add("is_central_operand_evaluated");
                }

                // оператор не имеет левого оператора или правого оператора
                if (!operands.containsKey(OperandPosition.LEFT)) {
                    set.add("left_operand_needed");
                } else if (!operands.containsKey(OperandPosition.RIGHT)) {
                    set.add("right_operand_needed");
                }

                if (!operands.containsKey(OperandPosition.LEFT) || !operands.containsKey(OperandPosition.RIGHT)) {
                    set.add("nearest_operand_needed");
                }

                // Посмотрим, в каких скобках данный оператор. Если ни в каких - тоже сохранить это как пару -1, -1
                var currentParen = tokens.getEnclosingParentheses(i);
                parentheses.add(currentParen);

                boolean hasDifferentPrec = false; // встречались операторы с разными приоритетами в направлении

                // Поиск ближайших операторов справа
                for (int j = i; j < tokens.size(); j++) {
                    // Запятая или другой разделитель останавливают поиск ближайшего оператора
                    if (tokens.get(j).type == TokenType.SEPARATOR || (tokens.get(j).type == TokenType.COMMA
                            && !(tokens.get(j) instanceof OperatorToken))) {
                        break;
                    }
                    if (tokens.get(j) instanceof OperatorToken nearOp && i != j) {
                        var paren = tokens.getEnclosingParentheses(j);
                        // Оператор в скобках, которые между двумя операторами
                        if (paren.getLeft() != -1 && i < paren.getLeft() && paren.getLeft() < j) {
                            continue;
                        }

                        // Оператор не должен содержаться в другом
                        if (op.isInOperandOf(nearOp, OperandPosition.CENTER) || nearOp.isInOperandOf(op, OperandPosition.CENTER)) {
                            continue;
                        }

                        boolean differentRightPrecedence = op.precedence != nearOp.precedence;
                        if (differentRightPrecedence && !hasDifferentPrec) {
                            set.add("order_determined_by_precedence");
                            hasDifferentPrec = true;
                            // Это не останавливает поиск, так как еще может быть оператор с различной ассоциативностью
                        }

                        var nearRightOperands = tokens.findOperands(j);
                        if (op.precedence == nearOp.precedence &&
                                (!nearRightOperands.containsKey(OperandPosition.LEFT) ||
                                !nearRightOperands.containsKey(OperandPosition.RIGHT))
                        ) {
                            set.add("associativity_without_opposing_operand");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.LEFT) {
                            set.add("order_determined_by_associativity");
                            set.add("left_competing_to_right_associativity");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.RIGHT) {
                            set.add("order_determined_by_associativity");
                            set.add("right_competing_to_left_associativity");
                            break;
                        }
                    }
                }

                // Поиск ближайших операторов слева
                for (int j = i; j >= 0; j--) {
                    // Запятая или другой разделитель останавливают поиск ближайшего
                    if (tokens.get(j).type == TokenType.SEPARATOR || (tokens.get(j).type == TokenType.COMMA
                            && !(tokens.get(j) instanceof OperatorToken))) {
                        break;
                    }
                    if (tokens.get(j) instanceof OperatorToken nearOp && i != j) {
                        var paren = tokens.getEnclosingParentheses(j);
                        // Оператор в скобках, которые между двумя операторами
                        if (paren.getLeft() != -1 && j < paren.getRight() && paren.getRight() < i) {
                            continue;
                        }

                        // Оператор не должен содержаться в другом
                        if (op.isInOperandOf(nearOp, OperandPosition.CENTER) || nearOp.isInOperandOf(op, OperandPosition.CENTER)) {
                            continue;
                        }

                        boolean differentLeftPrecedence = op.precedence != nearOp.precedence;
                        if (differentLeftPrecedence && !hasDifferentPrec) {
                            set.add("order_determined_by_precedence");
                            hasDifferentPrec = true;
                            // Это не останавливает поиск, так как еще может быть оператор с различной ассоциативностью
                        }

                        var nearLeftOperands = tokens.findOperands(j);
                        if (op.precedence == nearOp.precedence &&
                                (!nearLeftOperands.containsKey(OperandPosition.LEFT) ||
                                !nearLeftOperands.containsKey(OperandPosition.RIGHT))) {
                            set.add("associativity_without_opposing_operand");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.LEFT) {
                            set.add("order_determined_by_associativity");
                            set.add("left_competing_to_right_associativity");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.RIGHT) {
                            set.add("order_determined_by_associativity");
                            set.add("right_competing_to_left_associativity");
                            break;
                        }
                    }
                }

                // данный оператор - первый вычисляемый операнд оператора строгого порядка
                if (op.operandOf() != null && op.operandOf().isStrictOrder
                        && op.operandPosition().equals(op.operandOf().getFirstOperandToEvaluation())) {
                    set.add("strict_order_first_operand_to_be_evaluated");
                    set.add("is_first_operand_of_strict_order_operator_fully_evaluated");
                }

                boolean pyStrictOrderNoOmit = lang.equals(SupportedLanguage.PYTHON) && op.arity != OperatorArity.UNARY && !op.isStrictOrder;
                // запятая в Си или любой не унарный оператор в Python
                if ((op.isStrictOrder && op.type == TokenType.COMMA) || pyStrictOrderNoOmit) {
                    set.add("no_omitted_operands_despite_strict_order");
                }
                // все операторы строгого порядка
                if (op.operandOf() != null && op.operandOf().isStrictOrder
                        && op.operandOf().type != TokenType.COMMA
                        && op.operandPosition() != op.operandOf().getFirstOperandToEvaluation()
                ) {
                    set.add("should_strict_order_current_operand_be_omitted");
                }

                // только для строгого порядка вычисления аргументов функций в языке
                if (!lang.equals(SupportedLanguage.CPP)) {
                    // Нет операторов в аргументах вызова функции
                    if (op.type == TokenType.CALL_OPENING_BRACE
                            && tokens.findOperands(i)
                            .getOrDefault(OperandPosition.CENTER, new TokenGroup(0, 0, tokens))
                            .asSublist().stream().noneMatch(tok -> tok instanceof OperatorToken)
                    ) {
                        set.add("no_current_in_many_central_operands");
                    }

                    if (op.type == TokenType.CALL_OPENING_BRACE) {
                        // Найдем всю группу токенов, являющуюся аргументами вызова функций
                        var args = tokens.findOperands(i)
                                .getOrDefault(OperandPosition.CENTER,
                                        new TokenGroup(0, 0, tokens));
                        int argNum = 1; // номер аргумента в вызове функции
                        for (var argToken : args) {
                            if (argToken instanceof OperatorToken && argNum == 1) {
                                set.add("no_comma_in_central_operands");
                            }
                            if (argToken instanceof OperatorToken && argNum > 1) {
                                set.add("previous_central_operands_are_unevaluated");
                            }
                            if (argToken.type == TokenType.COMMA && !(argToken instanceof OperatorToken)) {
                                argNum++;
                            }
                        }
                    }
                }

                // Оператор не принадлежит оператору строгого порядка (не содержится в его операндах)
                if (op.operandOfHierarchy().stream().noneMatch(o -> o.getLeft().isStrictOrder)
                        && tokens.stream().anyMatch(o -> o instanceof OperatorToken op1 && op1.isStrictOrder)) {
                    set.add("is_current_operator_strict_order");
                }
            }
        }

        // Есть хотя бы один оператор в скобках, все выражение в скобках (и нигде больше скобок внутри) не считается
        if (parentheses.size() > 1) {
            set.add("order_determined_by_parentheses");
        }

        return set;
    }

    /**
     * Создание текста вопроса, рекомендуемое для вызова перед выдачей вопроса
     * @param tokens токены результирующего вопроса
     * @param domain домен, для которого генерируется
     * @param lang язык локализации пользователя
     * @return строка вопроса в HTML
     */
    static String questionToHtml(TokenList tokens,
                                 ProgrammingLanguageExpressionDTDomain domain,
                                 Language lang, int metaId
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(domain.getMessage("BASE_QUESTION_TEXT", lang));
        sb.append("<p class='comp-ph-expr'>");
        HashMap<Integer, Integer> complexEndingsIds = new HashMap<>();
        int idx = 0;
        int answerIdx = -1;
        for (Token t: tokens) {
            String tokenValue = t.value;
            if (t instanceof OperatorToken) {
                sb.append("<span data-comp-ph-pos='").append(++idx).append("' id='answer_")
                        .append(complexEndingsIds.containsKey(idx - 1) ? complexEndingsIds.get(idx - 1) : ++answerIdx)
                        .append("' class='comp-ph-expr-op-btn'").append(">").append(tokenValue).append("</span>");

                if (t instanceof ComplexOperatorToken complex && complex.isOpening()) {
                    int pos = tokens.findClosingComplex(idx - 1);
                    if (pos != -1) {
                        complexEndingsIds.put(pos, answerIdx);
                    }
                }
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
        sb.append("-->");
        sb.append("<!-- Metadata id: ");
        sb.append(metaId);
        sb.append("-->");
        sb.append("</p>");
        String text = sb.toString();

        sb = new StringBuilder(text
                .replaceAll("\\*", "&#8727")
                .replaceAll("\\n", "<br>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;"));
        sb.insert(0, "<div class='comp-ph-question'>"); sb.append("</div>");
        return sb.toString();
    }
}
