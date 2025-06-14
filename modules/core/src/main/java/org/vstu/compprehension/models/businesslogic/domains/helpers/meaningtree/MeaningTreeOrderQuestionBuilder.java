package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.common.MathHelper;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.common.Utils;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestionTemplate;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.exceptions.MeaningTreeException;
import org.vstu.meaningtree.iterators.utils.NodeInfo;
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
import org.vstu.meaningtree.utils.Label;
import org.vstu.meaningtree.utils.tokens.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/***
 * Builder of questions for expression domain in various formats (serializable or domain format).
 * Can build question from any syntax-correct expression of any SupportedLanguage by using the Meaning Tree (MT) library
 * Can be used by question generator
 * Can convert old expression domain to new MT format
 */
@Log4j2
public class MeaningTreeOrderQuestionBuilder {
    protected MeaningTree sourceExpressionTree = null; // initial expression in MT format (not mutations)
    protected QuestionMetadataEntity existingMetadata = null; // existing metadata (if existing question regenerates)

    // Additional information for question source
    protected String questionOrigin = null; // source of question (for example, source code repository full name)
    protected String originLicense = null; // license of source (for example, GPLv3)
    // Target domain for question is generated
    protected @Nullable ProgrammingLanguageExpressionDTDomain domain;

    // Preparing question data components
    protected List<SerializableQuestion.StatementFact> stmtFacts;
    protected List<SerializableQuestion.AnswerObject> answerObjects;
    protected SerializableQuestionTemplate.QuestionMetadata metadata;
    protected SerializableQuestion.QuestionData qdata;

    // Version of MT format
    protected static final int MIN_VERSION = 12;
    protected static final int TARGET_VERSION = 12;

    private boolean allChecksArePassed = true; // expression generator has failed some stages (questions won't be generated if false)
    private boolean skipRuntimeValuesGeneration = false;
    private boolean skipMutations = false;
    private boolean saveQuestionOnlyForSourceLanguage = false;

    @Getter
    protected @NotNull Set<SupportedLanguage> targetLanguages = SupportedLanguage.getMap().keySet();

    // default tags for each question in MT format
    private static final List<String> defaultQuestionTags = new ArrayList<>(
            List.of("basics", "operators", "order", "evaluation", "errors")
    );

    protected record Input(MeaningTree mt, int hash, TokenList tokens, String code) {}

    protected record ExpressionData(boolean allCorrect, TokenList tokens, String code) {};

    static {
        for (String lang : SupportedLanguage.getStringMap().keySet()) {
            defaultQuestionTags.add(lang.substring(0, 1).toUpperCase() + lang.substring(1));
        }
    }

    protected MeaningTreeOrderQuestionBuilder(@Nullable ProgrammingLanguageExpressionDTDomain domain) {
        this.domain = domain;
    }

    /***
     * Set existing question to regenerate
     * @param q question in domain format
     * @return builder
     */
    public MeaningTreeOrderQuestionBuilder existingQuestion(Question q) {
        MeaningTree mt;
        if (q.getMetadata().getVersion() >= MIN_VERSION) {
            mt = MeaningTreeRDFHelper.backendFactsToMeaningTree(
                    q.getQuestionData().getStatementFacts()
            );
        } else {
            log.info("Converting old-format question with metadata id={}", q.getMetadata().getId());
            mt = extractExpression(q.getQuestionData().getStatementFacts());
            if (mt != null) {
                allChecksArePassed = false;
            }
        }
        sourceExpressionTree = mt;
        existingMetadata = q.getMetadata();
        questionOrigin(q.getMetadata().getOrigin(), q.getMetadata().getOriginLicense());
        return this;
    }

    /**
     * Skip generation of runtime values for target expression
     * @param value value of this option
     * @return builder
     */
    public MeaningTreeOrderQuestionBuilder skipRuntimeValueGeneration(boolean value) {
        skipRuntimeValuesGeneration = value;
        return this;
    }

    /**
     * Skip generation of mutations for source expression tree
     * @param value value of this option
     * @return builder
     */
    public MeaningTreeOrderQuestionBuilder skipMutations(boolean value) {
        skipMutations = value;
        return this;
    }

    /**
     * Set languages for question generation
     * @param langs languages
     */
    public MeaningTreeOrderQuestionBuilder setTargetLanguages(@Nullable Set<SupportedLanguage> langs) {
        targetLanguages = Objects.requireNonNullElseGet(langs, () -> SupportedLanguage.getMap().keySet());
        return this;
    }

    /**
     * Deny generate questions for all possible languages
     * Generate will create question only in source language
     * @param state
     */
    public MeaningTreeOrderQuestionBuilder saveQuestionOnlyForSourceLanguage(boolean state) {
        saveQuestionOnlyForSourceLanguage = state;
        return this;
    }

    /**
     * Extract expression from old-format C++ question statement facts
     * @param facts old-format facts
     * @return meaning tree
     */
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
        var tokenizeResult = cppTranslator.getTokenizer().tryTokenizeExtended(tokens);
        if (tokenizeResult == null) {
            return null;
        }
        var tokenList = tokenizeResult.getRight();
        HashMap<TokenGroup, Object> semanticValuesIndexes = new HashMap<>();
        for (int i = 0; i < indexes.keySet().stream().max(Long::compare).orElse(0); i++) {
            if (semanticValues.containsKey(indexes.get(i))) {
                semanticValuesIndexes.put(new TokenGroup(i, i + 1, tokenList), semanticValues.get(indexes.get(i)));
            }
        }
        return cppTranslator.getMeaningTree(tokenList, semanticValuesIndexes);
    }

    /**
     * Set custom expression for question generation
     * @param expression code
     * @param language supported language in MT
     * @return builder
     */
    public MeaningTreeOrderQuestionBuilder expression(String expression, SupportedLanguage language) {
        try {
            sourceExpressionTree = language.createTranslator(new MeaningTreeDefaultExpressionConfig()).getMeaningTree(expression);
            return this;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Cannot create source translator with ".concat(language.toString()));
        }
    }

    /**
     * Recalculate only metadata of question. Useful for testing purposes
     * @param domain target DT domain
     * @param qMeta metadata entity from DB
     * @return new metadata entity
     */
    public static QuestionMetadataEntity metadataRecalculate(ProgrammingLanguageExpressionDTDomain domain,
                                                                                    QuestionMetadataEntity qMeta) {
        Question q = qMeta.getQuestionData().getData().toQuestion(domain, qMeta);
        MeaningTreeOrderQuestionBuilder builder = MeaningTreeOrderQuestionBuilder.newQuestion(domain).existingQuestion(q);
        SupportedLanguage language = MeaningTreeUtils.detectLanguageFromTags(qMeta.getTagBits(), domain);
        var data = builder.generateExpressionDataAccurate(builder.sourceExpressionTree, language);
        builder.allChecksArePassed &= data.allCorrect;
        if (!builder.allChecksArePassed) {
            return null;
        }
        builder.answerObjects = generateAnswerObjects(data.tokens());
        builder.processMetadata(language, new Input(
                builder.sourceExpressionTree, builder.sourceExpressionTree.hashCode(),
                data.tokens(), data.code()
        ));
        if (!builder.allChecksArePassed) {
            return null;
        }
        return builder.metadata.toMetadataEntity();
    }

    /**
     * Standalone generation of answer objects. Required in runtime question assignment
     * @param tokens tokens of expression
     * @return serializable answer objects for exercise
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
     * Initialize new question builder
     * @param domain target domain
     * @return builder
     */
    public static MeaningTreeOrderQuestionBuilder newQuestion(ProgrammingLanguageExpressionDTDomain domain) {
        MeaningTreeOrderQuestionBuilder builder = new MeaningTreeOrderQuestionBuilder(domain);
        return builder;
    }

    /**
     * Set existing meaning tree as expression source
     * @param mt meaning tree
     * @return builder
     */
    public MeaningTreeOrderQuestionBuilder meaningTree(MeaningTree mt) {
        sourceExpressionTree = mt;
        return this;
    }

    /**
     * Построить один вопрос из уже существующего без возвращения построителя вопроса
     * @param data сущность вопроса из приложения
     * @param lang язык программирования
     * @param domain домен, под который будет осуществляться генерация
     * @return сгенерированный вопрос
     */
    public static Question fastBuildFromExisting(Question data, SupportedLanguage lang, ProgrammingLanguageExpressionDTDomain domain) {
        var result = MeaningTreeOrderQuestionBuilder.newQuestion(domain).existingQuestion(data)
                .buildQuestions(lang);
        if (result.isEmpty()) {
            return null;
        }
        return result.getFirst();
    }

    /**
     * Set question origin info
     * @param name origin name (for example, repository full name)
     * @param license license of origin (for example, GPLv3)
     * @return builder
     */
    public MeaningTreeOrderQuestionBuilder questionOrigin(String name, String license) {
        questionOrigin = name;
        originLicense = license;
        return this;
    }


    /**
     * Build one or many question for given expression data
     * If one existing question with runtime values is given, one new format question will be prepared
     * If new expression is given and runtime value generation enabled, many new format question with various runtime data will be generated
     * @return one or many pairs of serializable question and its metadata
     */
    public List<Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata>> build(SupportedLanguage language) {
        answerObjects = new ArrayList<>();
        processTemplateStatementFacts();
        boolean bufAllChecksArePassed = allChecksArePassed;
        var result = generateManyQuestions(language);
        if (allChecksArePassed != bufAllChecksArePassed) {
            allChecksArePassed = bufAllChecksArePassed;
        }
        return result;
    }

    /**
     * Build all questions (many runtime values variants) in one serializable template
     * @return list of templates: in each template: one MT of question and much metadata for each supported language
     */
    public List<SerializableQuestionTemplate> buildAll() {
        List<SerializableQuestionTemplate> resultList = new ArrayList<>();

        List<Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata>> metadata = new ArrayList<>();

        for (SupportedLanguage language : targetLanguages) {
            Label origin = sourceExpressionTree.getLabel(Label.ORIGIN);
            if (origin != null && saveQuestionOnlyForSourceLanguage &&
                    !language.equals(SupportedLanguage.from((short) origin.getAttribute()))) {
                continue;
            }
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
     * Build all questions (with many runtime values) with one metadata for target language
     * @param lang target language for metadata
     * @return list of domain questions
     */
    public List<Question> buildQuestions(SupportedLanguage lang) {
        return build(lang).stream().map(
                (Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata> q) ->
                        q.getKey().toQuestion(domain, q.getValue().toMetadataEntity())).toList();
    }

    private String debugTokensString(MeaningTree mt, SupportedLanguage lang) {
        try {
            var tokRes = lang.createTranslator(new MeaningTreeDefaultExpressionConfig()).getTokenizer().tryTokenizeExtended(mt);
            var list = tokRes.getRight();
            if (!tokRes.getLeft()) {
                return "error:tokenizer";
            }

            StringBuilder builder = new StringBuilder();
            for (Token token : list) {
                builder.append(token.value);
                if (token.getAssignedValue() != null) {
                    builder.append("<-");
                    builder.append(token.getAssignedValue().toString());
                    builder.append(";>");
                }
                builder.append(' ');
            }
            return builder.toString();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException(e);
        }
    }

    /**
     * Generate one question for language
     * @param lang target language
     * @param input input data for generation
     * @return pair of serializable question and its metadata
     */
    protected Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata> generateFromTemplate(SupportedLanguage lang, Input input) {
        Model model = new RDFSerializer().serialize(input.mt);
        List<SerializableQuestion.StatementFact> facts = MeaningTreeRDFHelper.backendFactsToSerialized(
                MeaningTreeRDFHelper.factsFromModel(model));
        processMetadata(lang, input);
        processQuestionData(facts);
        SerializableQuestion serialized = SerializableQuestion.builder()
                .questionData(qdata)
                .concepts(List.of())
                .negativeLaws(List.of())
                .tags(defaultQuestionTags)
                .build();
        log.info("Created question: {}", debugTokensString(input.mt, lang));
        return new ImmutablePair<>(serialized, metadata);
    }

    /**
     * Формирование уникального вопроса из шаблона без дополнительных значений
     * @return сериализуемый вопрос
     */
    protected Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata> generateFromTemplate(SupportedLanguage lang) {
        var data = generateExpressionData(sourceExpressionTree, lang);
        allChecksArePassed &= data.allCorrect;
        if (!allChecksArePassed) {
            return null;
        }
        return generateFromTemplate(lang, new Input(
                sourceExpressionTree, sourceExpressionTree.hashCode(),
                data.tokens(), data.code()
        ));
    }

    /**
     * Generate many questions (for various runtime data) for target language
     * @param language target language
     * @return list of serializable questions and its metadata
     */
    protected List<Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata>> generateManyQuestions(SupportedLanguage language) {
        var initialData = generateExpressionData(sourceExpressionTree, language);
        allChecksArePassed &= initialData.allCorrect;
        if (!allChecksArePassed) {
            return List.of();
        }
        answerObjects = generateAnswerObjects(initialData.tokens());

        if (initialData.tokens().stream().anyMatch((Token t) -> t.getAssignedValue() != null) || skipRuntimeValuesGeneration) {
            log.debug("Given data already contains values paired with tokens");
            if (!allChecksArePassed) {
                return List.of();
            }
            var res = generateFromTemplate(language);
            return res == null ? List.of() : List.of(res);
        }
        List<Pair<SerializableQuestion, SerializableQuestionTemplate.QuestionMetadata>> generated = new ArrayList<>();

        List<MeaningTree> mutations;
        if (!skipMutations) {
            TreeMutationGenerator mutationGenerator = new TreeMutationGenerator(sourceExpressionTree);
            mutations = mutationGenerator.generate();
        } else {
            mutations = List.of(sourceExpressionTree);
        }

        for (MeaningTree mt : mutations) {
            var data = generateExpressionData(mt, language);
            if (!data.allCorrect) {
                continue;
            }
            OperandRuntimeValueGenerator map = new OperandRuntimeValueGenerator(this, mt, language);
            List<Pair<MeaningTree, Integer>> generatedValues = map.generate();
            for (Pair<MeaningTree, Integer> pair : generatedValues) {
                generated.add(generateFromTemplate(language, new Input(pair.getKey(), pair.getValue(),
                        data.tokens(), data.code())));
            }
        }
        if (generated.isEmpty()) {
            var defaultGen = generateFromTemplate(language);
            if (defaultGen != null) generated.add(defaultGen);
        }
        return !allChecksArePassed ? List.of() : generated;
    }

    /**
     * Obtains tokens and expr text of given expression
     * @param language target language
     */
    protected ExpressionData generateExpressionData(MeaningTree mt, SupportedLanguage language) {
        LanguageTranslator toTranslator;
        try {
            toTranslator = language.createTranslator(new MeaningTreeDefaultExpressionConfig());
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Cannot create source translator with ".concat(language.toString()));
        }
        var result = toTranslator.tryGetCode(mt);
        boolean checkSuccess = result.getLeft();
        String rawTranslatedCode = result.getRight();
        if (rawTranslatedCode != null) {
            var tokenRes = toTranslator.getTokenizer().tryTokenizeExtended(mt);
            checkSuccess &= tokenRes.getLeft();
            return new ExpressionData(checkSuccess, tokenRes.getLeft() ? tokenRes.getRight() : new TokenList(), rawTranslatedCode);

        } else {
            return new ExpressionData(checkSuccess, new TokenList(), "");
        }
    }

    /**
     * Process tokens with accurate check of translation compatibility
     * Works slowly than usual `generateExpressionData`
     * @param language target language
     */
    private ExpressionData generateExpressionDataAccurate(MeaningTree mt, SupportedLanguage language) {
        LanguageTranslator toTranslator;
        try {
            toTranslator = language.createTranslator(new MeaningTreeDefaultExpressionConfig());
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Cannot create source translator with ".concat(language.toString()));
        }
        var result = toTranslator.tryGetCode(mt);
        boolean checkSuccess = result.getLeft();
        String rawTranslatedCode = result.getRight();
        if (rawTranslatedCode != null) {
            var tokenRes = toTranslator.getTokenizer().tryTokenizeExtended(mt);
            checkSuccess &= tokenRes.getLeft();
            if (tokenRes.getLeft()) {
                var tokenRes2 = toTranslator.getTokenizer().tryTokenizeExtended(rawTranslatedCode);
                checkSuccess &= tokenRes2.getLeft();
            }
            return new ExpressionData(checkSuccess, tokenRes.getLeft() ? tokenRes.getRight() : new TokenList(), rawTranslatedCode);
        } else {
            return new ExpressionData(checkSuccess, new TokenList(), "");
        }
    }

    /**
     * Create question data from MT facts
     * @param facts MT facts
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
     * Create metadata from given question data
     * @param language target language
     * @param input input data for generation
     */
    protected void processMetadata(SupportedLanguage language, Input input) {
        if (domain == null && existingMetadata != null) {
            return;
        } else if (domain == null) {
            throw new MeaningTreeException("No valid data present for metadata");
        }
        QuestionMetadataEntity metadata = existingMetadata == null ? null : existingMetadata;
        List<String> tags = new ArrayList<>(List.of("basics", "operators", "order", "evaluation", "errors"));
        String languageStr = language.toString();
        tags.add(languageStr.substring(0, 1).toUpperCase() + languageStr.substring(1));
        Label nodeOrigin = input.mt.getLabel(Label.ORIGIN);
        if (nodeOrigin != null && !input.mt.hasLabel(Label.MUTATION_FLAG) && nodeOrigin.hasAttribute()
                && nodeOrigin.getAttribute().equals(language.getId())) {
            tags.add("original");
        }
        if (input.mt.hasLabel(Label.MUTATION_FLAG)) {
            tags.add("mutation");
        }

        if (questionOrigin == null || questionOrigin.isEmpty()) {
            throw new MeaningTreeException("Question origin didn't specified");
        }

        int omitted = findOmitted(input.mt, language);
        int solutionLength = answerObjects.size() - omitted;
        if (solutionLength <= 0) {
            solutionLength = 1;
        }

        var allConcepts = findAllConcepts(sourceExpressionTree.getRootNode(), language);
        var concepts = new HashSet<>(allConcepts);
        if (concepts.isEmpty() || solutionLength == 1) {
            allChecksArePassed = false;
        }
        var violations = findAllPossibleViolations(input.tokens);
        var skills = findAllSkills(input.tokens, language);
        var possibleViolations = new HashSet<>(violations);

        Set<String> possibleSkills = new HashSet<>(skills);

        // Filter question with repeated skills and violations
        final int stepsCheckThreshold = 12;
        final int maxSkillRepeatCount = 8;
        if (solutionLength > stepsCheckThreshold) {
            var counter = Utils.countElements(skills);
            for (var entry : counter.entrySet()) {
                if (entry.getValue() > maxSkillRepeatCount) {
                    allChecksArePassed = false;
                }
            }
            if (allChecksArePassed) {
                var conceptCounter = Utils.countElements(allConcepts);
                for (var entry : conceptCounter.entrySet()) {
                    if ((entry.getValue() / allConcepts.size()) > 0.9) {
                        allChecksArePassed = false;
                    }
                }
            }
        }
        double complexity = 0.18549906 * solutionLength - 0.01883239 * possibleViolations.size();
        complexity = MathHelper.sigmoid(complexity * 4 - 2);
        long conceptBits = concepts.stream().map(domain::getConcept).filter(Objects::nonNull).map(Concept::getBitmask).reduce((a, b) -> a|b).orElse(0L);

        String customTemplateId = StringHelper.truncate(input.code.replaceAll(
                " ", "_").replaceAll("[/:*?\"<>|\\\\]", ""),
                64).concat("_").concat(languageStr);
        if (input.mt.hasLabel(Label.MUTATION_FLAG)) {
            customTemplateId = customTemplateId.concat("mut_");
        }
        String customQuestionId = customTemplateId.concat(Integer.toString(input.hash)).concat("_v");

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
                .treeHashCode(input.hash)
                .language(language.toString())
                .structureHash(metadata != null ? metadata.getStructureHash() : "")
                .origin(questionOrigin)
                .originLicense(originLicense)
                .build();
    }

    /**
     * Find omitted operands count in Meaning Tree with assigned runtime values
     * @param tree meaning tree
     * @return omitted operands integer count
     */
    static int findOmitted(MeaningTree tree, SupportedLanguage lang) {
        int count = 0;
        HashSet<Node> visited = new HashSet<>();
        for (NodeInfo info : tree) {
            if (visited.contains(info)) {
                continue;
            }
            if (info.node() instanceof ShortCircuitAndOp op
                    && op.getLeft().getAssignedValueTag() instanceof Boolean bool && !bool) {
                count += countInternalOperators(op.getLeft(), lang);
                visited.add(op.getLeft());
            } else if (info.node() instanceof ShortCircuitOrOp op
                    && op.getLeft().getAssignedValueTag() instanceof Boolean bool && bool) {
                count += countInternalOperators(op.getLeft(), lang);
                visited.add(op.getLeft());
            } else if (info.node() instanceof TernaryOperator op) {
                if (op.getCondition().getAssignedValueTag() instanceof Boolean bool && !bool) {
                    count += countInternalOperators(op.getThenExpr(), lang);
                    visited.add(op.getThenExpr());
                } else {
                    count += countInternalOperators(op.getElseExpr(), lang);
                    visited.add(op.getElseExpr());
                }
            }
            visited.add(info.node());
        }
        return count;
    }

    /**
     * Create statement facts from Meaning Tree
     */
    protected void processTemplateStatementFacts() {
        RDFSerializer rdfSerializer = new RDFSerializer();
        Model m = rdfSerializer.serialize(sourceExpressionTree);
        this.stmtFacts = MeaningTreeRDFHelper.backendFactsToSerialized(MeaningTreeRDFHelper.factsFromModel(m));
    }

    /**
     * Find concept names in Meaning Tree
     * @param mt meaning tree
     * @param toLanguage target language
     * @return set of unique concept names of given expression
     */
    static Set<String> findConcepts(MeaningTree mt, SupportedLanguage toLanguage) {
        return new HashSet<>(findAllConcepts(mt.getRootNode(), toLanguage));
    }

    static int countInternalOperators(Node root, SupportedLanguage toLanguage) {
        var list = findAllConcepts(root, toLanguage);
        list.remove("operator_(");
        list.remove("operator_call");
        if (!list.isEmpty()) {
            return list.size() - 1;
        }
        return list.size();
    }

    static List<String> findAllConcepts(Node root, SupportedLanguage toLanguage) {
        ArrayList<String> result = new ArrayList<>();
        for (NodeInfo child : root) {
            Node node = child.node();
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
            else if (node instanceof PointerPackOp && toLanguage == SupportedLanguage.CPP) result.add("operator_unary_&");
            else if (node instanceof PointerUnpackOp && toLanguage == SupportedLanguage.CPP) result.add("operator_unary_*");
            else if (node instanceof PointerMemberAccess && toLanguage == SupportedLanguage.CPP) result.add("operator_->");
            else if (node instanceof PrefixIncrementOp) result.add("operator_prefix_++");
            else if (node instanceof PrefixDecrementOp) result.add("operator_prefix_--");
            else if (node instanceof PostfixIncrementOp) result.add("operator_postfix_++");
            else if (node instanceof PostfixDecrementOp) result.add("operator_postfix_--");
            else if (node instanceof MemberAccess) result.add("operator_.");
            else if (node instanceof TernaryOperator) result.add("operator_?");
            else if (node instanceof CommaExpression) result.add("operator_,");
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
     * Find possible violations in given expression
     * @param tokens tokens of expression
     * @return set of violation names
     */
    public static Set<String> findPossibleViolations(TokenList tokens) {
        return new HashSet<>(findAllPossibleViolations(tokens));
    }

    /**
     * Find all (contains repeats) possible violations in given expression
     * @param tokens tokens of expression
     * @return list of found violation names
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
                                op.assoc == OperatorAssociativity.RIGHT) {
                            set.add("associativity");
                            set.add("error_base_same_precedence_right_associativity_right");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.LEFT) {
                            set.add("associativity");
                            set.add("error_base_same_precedence_left_associativity_left");
                            break;
                        }
                    }
                }

                hasDifferentPrec = false;

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
                                op.assoc == OperatorAssociativity.RIGHT) {
                            set.add("associativity");
                            set.add("error_base_same_precedence_right_associativity_right");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.assoc == OperatorAssociativity.LEFT) {
                            set.add("associativity");
                            set.add("error_base_same_precedence_left_associativity_left");
                            break;
                        }
                    }
                }

                if (op.arity == OperatorArity.UNARY && op.assoc == OperatorAssociativity.LEFT) {
                    set.add("associativity");
                    set.add("error_base_unary_having_associativity_left");
                }

                if (op.arity == OperatorArity.UNARY && op.assoc == OperatorAssociativity.RIGHT) {
                    set.add("associativity");
                    set.add("error_base_unary_having_associativity_right");
                }

                if (op.arity == OperatorArity.BINARY && op.assoc == OperatorAssociativity.LEFT) {
                    set.add("associativity");
                    set.add("error_base_binary_having_associativity_left");
                }

                if (op.arity == OperatorArity.BINARY && op.assoc == OperatorAssociativity.RIGHT) {
                    set.add("associativity");
                    set.add("error_base_binary_having_associativity_right");
                }

                // операторы строгого порядка - не запятая: второй вычисляемый операнд
                if (op.operandOf() != null && op.operandOf().isStrictOrder
                        && op.operandOf().type != TokenType.COMMA
                        && op.operandPosition() != op.operandOf().getFirstOperandToEvaluation()
                ) {
                    set.add("error_base_student_error_unevaluated_operand");
                }

                // операнд оператора строгого порядка
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
     * Find  possible skills in given expression
     * @param tokens tokens of expression
     * @return set of found skill names
     */
    public static Set<String> findSkills(TokenList tokens, SupportedLanguage lang) {
        return new TreeSet<>(findAllSkills(tokens, lang));
    }

    /***
     * Determine which token in token list is parenthesized
     * @param op1 - first operator token index (not simple parentheses)
     * @param op2 - second operator token index (not simple parentheses)
     * @param tokens - expression tokens
     * @return index of parenthesized operator token, or else Integer.MAX_VALUE - if all are in parentheses, -1 - if all are not in parentheses
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
     * Find all (contains repeats) possible skills in given expression
     * @param tokens tokens of expression
     * @return list of found skill names
     */
    public static List<String> findAllSkills(TokenList tokens, SupportedLanguage lang) {
        List<String> set = new ArrayList<>();

        // Хотя бы один оператор, не являющийся комплексным (больше 1 токена)
        if (tokens.stream().anyMatch(t -> t instanceof OperatorToken && !(t instanceof ComplexOperatorToken))) {
            set.add("central_operand_needed");
        }

        // Ни одной операции строгого порядка
        if (tokens.stream().noneMatch(t -> t instanceof OperatorToken op && op.isStrictOrder)) {
            set.add("strict_order_operators_present");
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

                Map<OperandPosition, TokenGroup> operands;
                if (op instanceof ComplexOperatorToken cmplex && cmplex.positionOfToken == 1) {
                    operands = tokens.findOperands(tokens.findOpeningComplex(i));
                } else {
                    operands = tokens.findOperands(i);
                }

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

                        Map<OperandPosition, TokenGroup> nearRightOperands;
                        if (nearOp instanceof ComplexOperatorToken cmplex && cmplex.positionOfToken == 1) {
                            nearRightOperands = tokens.findOperands(tokens.findOpeningComplex(j));
                        } else {
                            nearRightOperands = tokens.findOperands(j);
                        }
                        if (op.precedence == nearOp.precedence &&
                                (!nearRightOperands.containsKey(OperandPosition.LEFT) ||
                                !nearRightOperands.containsKey(OperandPosition.RIGHT)) &&
                                (!operands.containsKey(OperandPosition.LEFT) ||
                                        !operands.containsKey(OperandPosition.RIGHT)) &&
                                !(op instanceof ComplexOperatorToken complex && nearOp instanceof ComplexOperatorToken complex2
                                        && (complex.positionOfToken != 1 || complex2.positionOfToken != 0 || tokens.findClosingComplex(j) == i))
                        ) {
                            set.add("associativity_without_opposing_operand");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.arity == OperatorArity.BINARY &&
                                op.assoc == OperatorAssociativity.RIGHT &&
                                !(op instanceof ComplexOperatorToken complex && nearOp instanceof ComplexOperatorToken complex2
                                    && (complex.positionOfToken != 1 || complex2.positionOfToken != 0 || tokens.findClosingComplex(j) == i))
                        ) {
                            set.add("order_determined_by_associativity");
                            set.add("left_competing_to_right_associativity");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.arity == OperatorArity.BINARY &&
                                op.assoc == OperatorAssociativity.LEFT &&
                                !(op instanceof ComplexOperatorToken complex && nearOp instanceof ComplexOperatorToken complex2
                                    && (complex.positionOfToken != 1 || complex2.positionOfToken != 0 || tokens.findClosingComplex(j) == i))
                        ) {
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

                        Map<OperandPosition, TokenGroup> nearLeftOperands;
                        if (nearOp instanceof ComplexOperatorToken cmplex && cmplex.positionOfToken == 1) {
                            nearLeftOperands = tokens.findOperands(tokens.findOpeningComplex(j));
                        } else {
                            nearLeftOperands = tokens.findOperands(j);
                        }
                        if (op.precedence == nearOp.precedence &&
                                (!nearLeftOperands.containsKey(OperandPosition.LEFT) ||
                                !nearLeftOperands.containsKey(OperandPosition.RIGHT)) &&
                                (!operands.containsKey(OperandPosition.LEFT) ||
                                        !operands.containsKey(OperandPosition.RIGHT)) &&
                                !(op instanceof ComplexOperatorToken complex && nearOp instanceof ComplexOperatorToken complex2
                                        && (complex.positionOfToken != 0 || complex2.positionOfToken != 0))
                        )
                        {
                            set.add("associativity_without_opposing_operand");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.arity == OperatorArity.BINARY &&
                                op.assoc == OperatorAssociativity.RIGHT &&
                                !(op instanceof ComplexOperatorToken complex && nearOp instanceof ComplexOperatorToken complex2
                                        && (complex.positionOfToken != 0 || complex2.positionOfToken != 0))) {
                            set.add("order_determined_by_associativity");
                            set.add("left_competing_to_right_associativity");
                            break;
                        }
                        if (op.precedence == nearOp.precedence &&
                                op.assoc == nearOp.assoc &&
                                op.arity == OperatorArity.BINARY &&
                                op.assoc == OperatorAssociativity.LEFT &&
                                !(op instanceof ComplexOperatorToken complex && nearOp instanceof ComplexOperatorToken complex2
                                        && (complex.positionOfToken != 0 || complex2.positionOfToken != 0))) {
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

}
