package org.vstu.compprehension.models.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.definition.EnumValueRef;
import its.model.definition.ObjectDef;
import its.model.nodes.DecisionTree;
import its.reasoner.LearningSituation;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.domains.helpers.ProgrammingLanguageExpressionsSolver;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.*;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestionTemplate;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.InteractionType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.serializers.rdf.RDFDeserializer;
import org.vstu.meaningtree.utils.tokens.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Log4j2
public class ProgrammingLanguageExpressionDTDomain extends DecisionTreeReasoningDomain {
    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + ProgrammingLanguageExpressionDomain.RESOURCES_LOCATION + "programming-language-expression-domain-dt-messages";
    static final String MESSAGE_PREFIX = "expr_domain_dt.";

    private final ProgrammingLanguageExpressionDomain baseDomain;
    private final LocalizationService localizationService;
    private final QuestionBank qMetaStorage;

    @SneakyThrows
    public ProgrammingLanguageExpressionDTDomain(DomainEntity domainEntity, ProgrammingLanguageExpressionDomain baseDomain) {
        super(domainEntity, baseDomain.randomProvider, null /* will be set after superclass call */);

        this.baseDomain = baseDomain;
        this.localizationService = baseDomain.localizationService;
        this.qMetaStorage = baseDomain.qMetaStorage;
        this.setBackendInterface(new DecisionTreeInterface()); // TODO у dt циклическая зависимость с доменом, нужно это пересмотреть

        this.concepts = baseDomain.concepts;
        this.positiveLaws = baseDomain.positiveLaws;
        this.negativeLaws = baseDomain.negativeLaws;
        fillSkills();
    }

    private static final HashMap<String, Tag> tags = new HashMap<>() {{
        put("C++", new Tag("C++", 1L));    // (2 ^ 0)
        put("basics", new Tag("basics", 2L));    // (2 ^ 1)
        put("errors", new Tag("errors", 4L));    // (2 ^ 2)
        put("evaluation", new Tag("evaluation", 8L));    // (2 ^ 3)
        put("operators", new Tag("operators", 16L));    // (2 ^ 4)
        put("order", new Tag("order", 32L));    // (2 ^ 5)
        put("Python", new Tag("Python", 64L));    // (2 ^ 6)
        put("Java", new Tag("Java", 128L));    // (2 ^ 7)
        put("mutation", new Tag("mutation", 256L));    // (2 ^ 8)
        put("original", new Tag("original", 512L));    // (2 ^ 9)
    }};

    @NotNull
    @Override
    public Map<String, Tag> getTags() {
        return tags;
    }


    public void fillSkills() {
        skills = new HashMap<>();

        addSkill("central_operand_needed", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("is_central_operand_evaluated", Skill.FLAG_VISIBLE_TO_TEACHER);

        Skill nearestOperandNeeded = addSkill("nearest_operand_needed", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("left_operand_needed", List.of(nearestOperandNeeded));
        addSkill("right_operand_needed", List.of(nearestOperandNeeded));

        Skill competingOperandPresent = addSkill("competing_operator_present", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("left_competing_operator_present", List.of(competingOperandPresent));
        addSkill("right_competing_operator_present", List.of(competingOperandPresent));

        Skill currentOperatorEnclosed = addSkill("current_operator_enclosed", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("left_operator_enclosed", List.of(currentOperatorEnclosed));
        addSkill("right_operator_enclosed", List.of(currentOperatorEnclosed));

        Skill parenthesizedSkills = addSkill("order_determined_by_parentheses", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("is_current_parenthesized_left_not", List.of(parenthesizedSkills));
        addSkill("is_current_parenthesized_right_not", List.of(parenthesizedSkills));
        addSkill("is_left_parenthesized_current_not", List.of(parenthesizedSkills));
        addSkill("is_right_parenthesized_current_not", List.of(parenthesizedSkills));

        Skill prec = addSkill("order_determined_by_precedence", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("left_competing_to_right_precedence", List.of(prec));
        addSkill("right_competing_to_left_precedence", List.of(prec));

        Skill assoc = addSkill("order_determined_by_associativity");
        addSkill("left_competing_to_right_associativity", List.of(assoc), Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("right_competing_to_left_associativity", List.of(assoc), Skill.FLAG_VISIBLE_TO_TEACHER);

        Skill associativityWithoutOpposingOperand = addSkill("associativity_without_opposing_operand", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("associativity_without_left_opposing_operand", List.of(associativityWithoutOpposingOperand));
        addSkill("associativity_without_right_opposing_operand", List.of(associativityWithoutOpposingOperand));

        Skill strictOrder = addSkill("strict_order_operators_present", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("expression_strict_order_operators_present", List.of(strictOrder));
        addSkill("earlyfinish_strict_order_operators_present", List.of(strictOrder));

        Skill currentStrictOrder = addSkill("is_current_operator_strict_order", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill(currentStrictOrder.name + "_while_solving", List.of(currentStrictOrder));
        addSkill(currentStrictOrder.name + "_while_earlyfinish", List.of(currentStrictOrder));

        Skill strictOrderFirstOperandToBeEvaluated = addSkill("strict_order_first_operand_to_be_evaluated", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill(strictOrderFirstOperandToBeEvaluated.name + "_while_solving", List.of(strictOrderFirstOperandToBeEvaluated));
        addSkill(strictOrderFirstOperandToBeEvaluated.name + "_while_earlyfinish", List.of(strictOrderFirstOperandToBeEvaluated));

        addSkill("is_first_operand_of_strict_order_operator_fully_evaluated", Skill.FLAG_VISIBLE_TO_TEACHER);

        Skill noOmittedOperandsDespiteStrictOrder = addSkill("no_omitted_operands_despite_strict_order", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill(noOmittedOperandsDespiteStrictOrder.name + "_while_solving", List.of(noOmittedOperandsDespiteStrictOrder));
        addSkill(noOmittedOperandsDespiteStrictOrder.name + "_while_earlyfinish", List.of(noOmittedOperandsDespiteStrictOrder));

        Skill shouldStrictOrderCurrentOperandBeOmitted = addSkill("should_strict_order_current_operand_be_omitted", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill(shouldStrictOrderCurrentOperandBeOmitted.name + "_while_solving", List.of(shouldStrictOrderCurrentOperandBeOmitted));
        addSkill(shouldStrictOrderCurrentOperandBeOmitted.name + "_while_earlyfinish", List.of(shouldStrictOrderCurrentOperandBeOmitted));

        addSkill("are_central_operands_strict_order", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("no_current_in_many_central_operands", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("no_comma_in_central_operands", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("previous_central_operands_are_unevaluated", Skill.FLAG_VISIBLE_TO_TEACHER);

        fillSkillTree();

        // assign mask bits to Skills
        val name2bit = _getSkillsName2bit();
        for (Skill t : skills.values()) {
            val name = t.getName();
            if (name2bit.containsKey(name)) {
                t.setBitmask(name2bit.get(name));
            } else {
                throw new RuntimeException("Invalid bitmask for skill " + name);
            }
        }
    }

    /** Set direct children to skills. This is needed since parents (bases) of skills are stored only */
    protected void fillSkillTree() {
        for (Skill skill : skills.values()) {
            if (skill.getBaseSkills() == null)
                continue;
            for (Skill base : skill.getBaseSkills()) {
                if (base.getChildSkills() == null) {
                    base.setChildSkills(new HashSet<>());
                }
                base.getChildSkills().add(skill);
            }
        }
    }

    @NotNull
    @Override
    public String getShortnameForQuestionSearch() {
        return "expression_dt";
    }

    @Override
    public QuestionRequest ensureQuestionRequestValid(QuestionRequest questionRequest) {
        return baseDomain.ensureQuestionRequestValid(questionRequest);
    }

    @Override
    public @NotNull Question makeQuestion(@NotNull QuestionRequest questionRequest,
                                          @NotNull ExerciseAttemptEntity exerciseAttempt,
                                          @NotNull Language userLanguage) {
        SupportedLanguage lang = MeaningTreeUtils.detectLanguageFromTags(questionRequest.getTargetTags().stream().map(Tag::getName).toList());

        return QuestionDynamicDataAppender.appendQuestionData(baseDomain.makeQuestion(questionRequest, exerciseAttempt, userLanguage, this), exerciseAttempt, qMetaStorage, lang, this, userLanguage);
    }

    @Override
    public @NotNull Question makeQuestion(@NotNull QuestionMetadataEntity metadata,
                                          @Nullable ExerciseAttemptEntity exerciseAttemptEntity,
                                          @NotNull List<Tag> tags,
                                          @NotNull Language userLang) {
        SupportedLanguage lang = MeaningTreeUtils.detectLanguageFromTags(tags.stream().map(Tag::getName).toList());

        return QuestionDynamicDataAppender.appendQuestionData(baseDomain.makeQuestion(metadata, exerciseAttemptEntity, tags, userLang, this), exerciseAttemptEntity, qMetaStorage, lang, this, userLang);
    }

    public static final String DOMAIN_MODEL_LOCATION = ProgrammingLanguageExpressionDomain.RESOURCES_LOCATION + "programming-language-expression-domain-model/";
    private final DomainSolvingModel domainSolvingModel = new DomainSolvingModel(
            this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION), //FIXME
            DomainSolvingModel.BuildMethod.LOQI
    ).validate();

    @Override
    public DomainSolvingModel getDomainSolvingModel() {
        return domainSolvingModel;
    }

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

    @Override
    public List<Law> getQuestionLaws(String questionDomainType, List<Tag> tags) {
        throw new UnsupportedOperationException("no Laws are used for " + this.getClass().getSimpleName());
    }

    @Override
    protected List<Question> getQuestionTemplates() {
        return baseDomain.getQuestionTemplates();
    }

    // filter positive laws by question type and tags
    @Override
    public List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(ProgrammingLanguageExpressionDomain.EVALUATION_ORDER_QUESTION_TYPE) || questionDomainType.equals(ProgrammingLanguageExpressionDomain.DEFINE_TYPE_QUESTION_TYPE) || questionDomainType.equals(ProgrammingLanguageExpressionDomain.OPERANDS_TYPE_QUESTION_TYPE)
                || questionDomainType.equals(ProgrammingLanguageExpressionDomain.PRECEDENCE_TYPE_QUESTION_TYPE)) {
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

    @Override
    public List<NegativeLaw> getNegativeLaws() {
        return baseDomain.getNegativeLaws();
    }

    @Override
    public String getDefaultQuestionType(boolean supplementary) {
        return baseDomain.getDefaultQuestionType(supplementary);
    }

    @Override
    public List<Tag> getDefaultQuestionTags(String questionDomainType) {
        return baseDomain.getDefaultQuestionTags(questionDomainType);
    }

    public List<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        throw new UnsupportedOperationException("no Laws are used for " + this.getClass().getSimpleName());
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

    //-----Суждение вопросов и подобное ------

    public Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return new HashSet<>(); //Не нужно для DT
    }

    public Set<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return new HashSet<>(); //Не нужно для DT
    }

    private static final String STILL_UNEVALUATED_LEFT_VIOLATION_NAME = "stillUnevaluatedLeft";

    private class DecisionTreeInterface extends DecisionTreeReasonerBackend.Interface {
        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForJudge(
                Question question,
                List<ResponseEntity> responses,
                List<Tag> tags
        ) {
            return new DecisionTreeReasonerBackend.Input(
                    MeaningTreeRDFTransformer.questionToDomainModel(
                            domainSolvingModel, question.getStatementFacts(), responses, tags
                    ),
                    domainSolvingModel.getDecisionTree()
            );
        }

        @Override
        protected InterpretSentenceResult interpretJudgeNotPerformed(
                Question judgedQuestion,
                LearningSituation preparedSituation
        ) {
            ProgrammingLanguageExpressionsSolver solver = new ProgrammingLanguageExpressionsSolver();
            ProgrammingLanguageExpressionsSolver.SolveResult solveResult = solver.solveNoVars(preparedSituation.getDomainModel(),
                    domainSolvingModel.decisionTree("earlyfinish")
            );

            ViolationEntity violation = new ViolationEntity();
            violation.setLawName(STILL_UNEVALUATED_LEFT_VIOLATION_NAME);
            violation.setViolationFacts(new ArrayList<>());
            InterpretSentenceResult result = new InterpretSentenceResult();
            result.violations = new ArrayList<>(List.of(violation));

            result.explanation = DecisionTreeReasonerBackend.collectExplanationsFromTrace(
                    Explanation.Type.ERROR, solveResult.trace(),
                    preparedSituation.getDomainModel(), getUserLanguageByQuestion(judgedQuestion));
            result.violations.addAll(result.explanation.getDomainLawNames().stream().map(skill -> {
                ViolationEntity v = new ViolationEntity();
                v.setLawName(skill);
                v.setViolationFacts(new ArrayList<>());
                return v;
            }).toList());
            updateInterpretationResult(result, preparedSituation);
            return result;
        }

        @Override
        protected void updateJudgeInterpretationResult(
                InterpretSentenceResult interpretationResult,
                DecisionTreeReasonerBackend.Output backendOutput
        ) {
            updateInterpretationResult(interpretationResult, backendOutput.situation());
        }

        private void updateInterpretationResult(
                InterpretSentenceResult interpretationResult,
                LearningSituation situation
        ) {
            interpretationResult.CountCorrectOptions = 1; //TODO? Непонятно зачем оно надо
            interpretationResult.IterationsLeft = calculateLeftInteractions(situation);

            if (interpretationResult.IterationsLeft == 0) {
                // Достигли полного завершения задачи.
                // Ошибок уже быть не может — сбросим их все.
                interpretationResult.isAnswerCorrect = true;
                interpretationResult.violations = List.of();
                interpretationResult.explanation = Explanation.empty(Explanation.Type.HINT);
            }
        }

        public int calculateLeftInteractions(LearningSituation situation) {
            int unevaluatedCount = (int) situation.getDomainModel().getObjects()
                    .stream().filter(objectDef ->
                            hasState(objectDef, "unevaluated")
                    )
                    .count();
            // Добавляем количество таких, о которых уже известно,
            // что выполняться они не будут, но их родитель ещё не выполнился,
            // и нам не нужно давать студенту подсказку об этом через видимое число оставшихся шагов.
            int omittedDistractorCount = (int) situation.getDomainModel().getObjects()
                    .stream().filter(objectDef ->
                            (hasState(objectDef, "omitted")
                                    && getParent(objectDef).map(parent -> hasState(parent, "unevaluated")).orElse(false))
                    )
                    .count();

            int omittedCount = (int) situation.getDomainModel().getObjects()
                    .stream().filter(objectDef ->
                            hasState(objectDef, "omitted")
                    )
                    .count();

            // Эвристика, может давать сбои: если студент нажал "ничего больше не выполнится",
            // то дерево не запускалось и его переменные не задавались.
            boolean endEvaluationClicked = situation.getDecisionTreeVariables().isEmpty();

            // Потребовать нажать кнопку "ничего больше не выполнится", если есть корректно опущенные операторы (чтобы проверить, понимает ли студент это).
            int oneMoreStepForEndEvaluation = omittedCount > 0 && !endEvaluationClicked ? 1 : 0;

            return unevaluatedCount + omittedDistractorCount + oneMoreStepForEndEvaluation;
        }

        private Optional<ObjectDef> getParent(ObjectDef object) {
            return object.getRelationshipLinks().listByName("isOperandOf").stream().findFirst()
                    .map(link -> link.getObjects().get(0));
        }

        private boolean hasState(ObjectDef object, String stateValueName) {
            if (!object.isInstanceOf("operand")) {
                return false;
            }
            return new EnumValueRef("state", stateValueName).equals(object.getPropertyValue("state", Map.of()));
        }

        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForSolve(Question question, List<Tag> tags) {
            return null; //Solve not used in DecisionTreeReasonerBackend
        }

        @Override
        public String getBackendId() {
            return DecisionTreeReasonerBackend.BACKEND_ID;
        }
    }

    protected static String oldQuestionModelToTokens(List<BackendFactEntity> facts) {
        Map<Integer, String> indexes = new HashMap<>();
        Map<String, String> tokenValues = new HashMap<>();
        StringBuilder tokenBuilder = new StringBuilder();

        for (BackendFactEntity st : facts) {
            if (st.getVerb().equals("index")) {
                indexes.put(Integer.parseInt(st.getObject()), st.getSubject());
            } else if (st.getVerb().equals("text")) {
                tokenValues.put(st.getSubject(), st.getObject());
            }
        }
        for (int i = 0; i <= indexes.keySet().stream().max(Long::compare).orElse(0); i++) {
            if (indexes.containsKey(i)) {
                tokenBuilder.append(tokenValues.get(indexes.get(i)));
                tokenBuilder.append(' ');
            }
        }
        return tokenBuilder.substring(0, tokenBuilder.length() - 1);
    }

    /**
     * Note! It saves result file to specified directory, not to location where question storage usually reads questions from.
     * @param templatePaths
     * @param outputDir
     * @param questionsLimit
     * @param origin
     */
    public void generateManyQuestions(List<String> templatePaths, String outputDir,
                                      int questionsLimit, String origin
    ) {
        int count = 0;  // templates
        int qCount = 0;
        int savedCount = 0;
        // TODO: please set value of this var to null in production code. Temporary changes
        Set<SupportedLanguage> targetLanguages = null;

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

        for (String file : templatePaths) {
            file = file.replaceAll("\\\\","/");
            log.info("Start generating question(s) for template {}", file);
            try {
                if (qCount > questionsLimit)
                    break;

                Path path = Path.of(file);
                String parsedQuestionName = path.getFileName().toString();

                SupportedLanguage currentLang = SupportedLanguage.CPP;
                String expressionText = null;
                MeaningTreeOrderQuestionBuilder builder = null;
                String license = null;

                if (parsedQuestionName.endsWith(".mt.ttl")) {
                    String[] lines = Files.readString(Path.of(file)).split("\n");
                    for (String line : lines) {
                        if (line.strip().startsWith("# LICENSE ")) {
                            license = line.strip().replaceFirst("# LICENSE ", "");
                        } else if (line.strip().startsWith("# REPO_NAME ")) {
                            origin = line.strip().replaceFirst("# REPO_NAME ", "");
                        }
                    }

                    RDFDeserializer deserializer = new RDFDeserializer();
                    Model templateModel = ModelFactory.createDefaultModel();
                    RDFDataMgr.read(templateModel, file);
                    MeaningTree mt = deserializer.deserializeTree(templateModel);
                    for (SupportedLanguage language : SupportedLanguage.getMap().keySet()) {
                        String languageStr = language.toString().toLowerCase();
                        if (parsedQuestionName.endsWith(String.format("_%s.mt.ttl", languageStr))) {
                            currentLang = language;
                            break;
                        }
                    }

                    builder = MeaningTreeOrderQuestionBuilder.newQuestion(this)
                            .meaningTree(mt)
                            .setTargetLanguages(targetLanguages)
                            .questionOrigin(origin, license);
                } else if (parsedQuestionName.endsWith(".ttl")) {
                    for (SupportedLanguage language : SupportedLanguage.getMap().keySet()) {
                        String languageStr = language.toString().toLowerCase();
                        if (parsedQuestionName.endsWith(String.format("%s.ttl", languageStr))) {
                            currentLang = language;
                            break;
                        }
                    }
                    Model templateModel = ModelFactory.createDefaultModel();
                    RDFDataMgr.read(templateModel, file);
                    Model domainSchemaModel = baseDomain.getFullSchema();
                    Model targetModel = domainSchemaModel
                            .union(templateModel);
                    expressionText = oldQuestionModelToTokens(MeaningTreeRDFHelper.factsFromModel(targetModel));
                } else if (parsedQuestionName.endsWith(".json")) {
                    try {
                        QuestionGeneratedTemplate q = gson.fromJson(Files.readString(path), QuestionGeneratedTemplate.class);
                        if (q.getTokens() != null && !q.getTokens().isEmpty()) {
                            expressionText = String.join(" ", q.getTokens());
                        } else if (q.getText() != null){
                            expressionText = q.getText();
                        } else {
                            log.warn("Empty question: required tokens or text in json file");
                            continue;
                        }
                        currentLang = SupportedLanguage.fromString(q.getLanguage());
                    } catch (JsonSyntaxException | IOException e) {
                        log.error("Skip a json file due to error: ", e);
                        continue;
                    }
                } else {
                    log.info("Skipping non-question file: {}", path);
                    continue;
                }

                parsedQuestionName = parsedQuestionName.substring(0, parsedQuestionName.length() - ".ttl".length());
                parsedQuestionName = parsedQuestionName.replaceAll("[^a-zA-Z0-9_=+-]", "");

                var templateName = parsedQuestionName;
                templateName = templateName.replaceAll("__\\d{10}$", ""); // remove timestamp

                count++;

                // Create a template
                log.debug("{} \tUpload model number {}", templateName, count);

                log.debug("Creating questions for template: {}", templateName);
                if (builder == null) {
                    builder =
                            MeaningTreeOrderQuestionBuilder
                                    .newQuestion(this)
                                    .setTargetLanguages(targetLanguages)
                                    .expression(expressionText, currentLang)
                                    .questionOrigin(origin, license);
                }

                int templateQuestionsCount = 0;
                for (SerializableQuestionTemplate template : builder.buildAll()) {
                    String fileName = Integer.toString(template.getMetadataList().getFirst().getTreeHashCode());
                    template.serializeToFile(Path.of(outputDir, "question_" + fileName + ".json"));
                    ++templateQuestionsCount;
                }

                if (templateQuestionsCount > 0) {
                    log.info("Successfully generated {} question(s) for template {}", templateQuestionsCount, file);
                }
            } catch (Exception e) {
                log.error("Generator exception {} on {} with msg: {}", file, e.getClass().getName(), e.getMessage(), e);
            }
        }

        log.info("Total questions generated: {} ({} saved).\n", qCount, savedCount);
    }

    @Override
    public Collection<Fact> responseToFacts(
            String questionDomainType,
            List<ResponseEntity> responses,
            List<AnswerObjectEntity> answerObjects
    ) {
        if (questionDomainType.equals(ProgrammingLanguageExpressionDomain.EVALUATION_ORDER_QUESTION_TYPE)) {
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
                            ProgrammingLanguageExpressionDomain.END_EVALUATION,
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
        } else if (questionDomainType.equals(ProgrammingLanguageExpressionDomain.DEFINE_TYPE_QUESTION_TYPE)) {
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
        } else if (questionDomainType.equals(ProgrammingLanguageExpressionDomain.OPERANDS_TYPE_QUESTION_TYPE)) {
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
        } else if (questionDomainType.equals(ProgrammingLanguageExpressionDomain.PRECEDENCE_TYPE_QUESTION_TYPE)) {
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
    public Collection<Fact> getQuestionStatementFactsWithSchema(Question q) {
        return baseDomain.getQuestionStatementFactsWithSchema(q);
    }

    @Override
    public List<HyperText> getFullSolutionTrace(Question question) {
        Language lang = Optional.ofNullable(question.getQuestionData().getExerciseAttempt())
            .map(a -> a.getUser().getPreferred_language())
            .orElse(Language.RUSSIAN/*ENGLISH*/);
        SupportedLanguage plang = MeaningTreeUtils.detectLanguageFromTags(question.getMetadata().getTagBits(), this);

        ArrayList<HyperText> result = new ArrayList<>();

        String qType = question.getQuestionData().getQuestionDomainType();
        if (qType.equals(ProgrammingLanguageExpressionDomain.EVALUATION_ORDER_QUESTION_TYPE)) {
            TokenList tokens = MeaningTreeRDFHelper.backendFactsToTokens(question.getStatementFacts(), plang);

            for (ResponseEntity response : baseDomain.responsesForTrace(question.getQuestionData(), true)) {
                // format a trace line ...
                AnswerObjectEntity answerObj = response.getLeftAnswerObject();
                String domainInfo = answerObj.getDomainInfo();
                if (domainInfo.equals("end_token")) {
                    continue;
                }
                StringJoiner builder = new StringJoiner(" ");
                String[] domainInfoComponents = domainInfo.split("_");
                int tokenIndex = Integer.parseInt(domainInfoComponents[domainInfoComponents.length - 1]);
                int tokenPositionInUI = tokenIndex + 1;
                Token mainToken = tokens.get(tokenIndex);
                Token pairedToken = null;
                if (mainToken instanceof ComplexOperatorToken) {
                    int closingTokenIndex = tokens.findClosingComplex(tokenIndex);
                    pairedToken = closingTokenIndex > 0 && closingTokenIndex < tokens.size() ?
                            tokens.get(closingTokenIndex) : null;
                }
                String tokenType = switch (mainToken.type) {
                    case CALL_OPENING_BRACE -> getMessage("FUNC_CALL", lang);
                    case INITIALIZER_LIST_OPENING_BRACE ->  getMessage("LITERAL", lang);
                    default -> getMessage("OPERATOR", lang);
                };
                String tokensRepr = mainToken.value + (pairedToken != null ? " ".concat(pairedToken.value) : "");
                builder.add("<span>" + tokenType + "</span>");
                builder.add("<span style='color: #700;'>" +
                        tokensRepr +
                        "</span>");
                builder.add("<span>" + getMessage("AT_POS", lang) + "</span>");
                builder.add("<span style='color: #f00;font-weight: bold;'>" +
                        tokenPositionInUI +
                        "</span>");
                builder.add("<span>" + getMessage("CALCULATED", lang) + "</span>");

                boolean responseIsWrong = !response.getInteraction().getViolations().isEmpty();

                if (!responseIsWrong) {
                    // Show the value only if this is a correct choice.
                    Token t = tokens.get(tokenIndex);
                    Object value = t.getAssignedValue();
                    if (value == null && t instanceof OperatorToken op
                            && (op.additionalOpType == OperatorType.OR ||
                                op.additionalOpType == OperatorType.AND)) {
                        value = false;
                    }
                    if (value != null) {
                        builder.add("<span>" + getMessage("WITH_VALUE", lang) + "</span>");
                        builder.add("<span style='color: #f08;font-style: italic;font-weight: bold;'>" +
                                value +
                                "</span>");
                    }
                }

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

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        Language lang = Optional.ofNullable(q.getQuestionData().getExerciseAttempt())
                .map(a -> a.getUser().getPreferred_language())
                .orElse(Language.RUSSIAN/*ENGLISH*/);

        Optional<InteractionEntity> lastCorrectInteraction = Optional.ofNullable(q.getQuestionData().getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().isEmpty())
                .reduce((first, second) -> second);
        List<ResponseEntity> responses = new ArrayList<>();
        lastCorrectInteraction.ifPresent(interactionEntity -> responses.addAll(interactionEntity.getResponses()));
        List<Integer> responseTokenIndexes = responses.stream()
                .map(res ->
                        answerObjectToTokenIndex(res.getLeftAnswerObject()))
                .toList();
        List<Tag> tags = q.getTags();
        DomainModel domain = MeaningTreeRDFTransformer.questionToDomainModel(
                domainSolvingModel, q.getStatementFacts(), responses, tags, false
        );
        DecisionTree dt = domainSolvingModel.getDecisionTree();
        ProgrammingLanguageExpressionsSolver solver = new ProgrammingLanguageExpressionsSolver();
        // Проверить в ризонере все возможные варианты интеракций и понять, какая из них правильная и выдать подсказку
        Optional<Pair<ObjectDef, ProgrammingLanguageExpressionsSolver.SolveResult>> found = domain.getObjects().stream()
                .filter(domainObj ->
                    domainObj.getClazz().isSubclassOf("operator") && domainObj.getRelationshipLinks().stream().filter(rel ->rel.getRelationshipName().equals("has")).allMatch(
                            rel -> rel.getObjects().stream().noneMatch(obj ->
                                    responseTokenIndexes.contains((Integer) obj.getMetadata().get("index")))
                    )
                ).map(
                        domainObj -> Pair.of(domainObj, solver.solveForX(domainObj, domain, dt))
                ).filter(
                        pair -> pair.getRight().solved()
                )
                .findFirst();
        if (found.isPresent()) {
            var solveRes = found.get().getRight();
            String[] objName = found.get().getLeft().getName().split("_");
            int tokenPos = Integer.parseInt(objName[objName.length - 1]);
            for (AnswerObjectEntity answer : q.getAnswerObjects()) {
                if (answer.getDomainInfo().endsWith(String.valueOf(tokenPos))) {

                    Explanation explanation = DecisionTreeReasonerBackend.collectExplanationsFromTrace(
                            Explanation.Type.HINT,
                            solveRes.trace(), domain,
                            lang
                    );
                    if (explanation.isEmpty()) {
                        explanation.getChildren().add(new Explanation(Explanation.Type.HINT, new HyperText(
                                        getMessage("explanations.missing_correct_answer_explanation", lang))));
                    }

                    CorrectAnswer correctAnswer = new CorrectAnswer();
                    correctAnswer.answers = List.of(new CorrectAnswer.Response(answer, answer));
                    correctAnswer.question = q.getQuestionData();
                    correctAnswer.lawName = null;
                    correctAnswer.skillName = solveRes.skills();
                    correctAnswer.explanation = explanation;
                    return correctAnswer;
                }
            }
        }
        AnswerObjectEntity everythingIsEvaluated = q.getAnswerObjects().getLast();
        CorrectAnswer correctAnswer = new CorrectAnswer();
        correctAnswer.answers = List.of(new CorrectAnswer.Response(everythingIsEvaluated, everythingIsEvaluated));
        correctAnswer.question = q.getQuestionData();
        correctAnswer.lawName = null;
        correctAnswer.skillName = List.of();
        correctAnswer.explanation = DecisionTreeReasonerBackend.collectExplanationsFromTrace(Explanation.Type.HINT,
                solver.solveNoVars(domain, domainSolvingModel.decisionTree("earlyfinish")).trace(), domain, lang
        );
        return correctAnswer;
    }

    private int answerObjectToTokenIndex(AnswerObjectEntity ans) {
        String domainInfo = ans.getDomainInfo();
        if (!domainInfo.isEmpty() && Character.isDigit(domainInfo.charAt(domainInfo.length() - 1))) {
            return Integer.parseInt(domainInfo.split("_")[1]);
        }
        return -1;
    }

    //----------Вспомогательные вопросы------------

    @Override
    public boolean needSupplementaryQuestion(ViolationEntity violation) {
        Skill skill = getSkill(violation.getLawName());
        return skill != null && violation.getInteraction().getInteractionType() != InteractionType.REQUEST_CORRECT_ANSWER;
    }

    private DomainModel mainQuestionToModel(InteractionEntity lastMainQuestionInteraction) {
        List<Tag> tags = lastMainQuestionInteraction.getQuestion().getTags().stream().map(this::getTag).filter(Objects::nonNull).toList();
        return MeaningTreeRDFTransformer.questionToDomainModel(
                domainSolvingModel,
                new Question(lastMainQuestionInteraction.getQuestion(), this).getStatementFacts(),
                lastMainQuestionInteraction.getResponses(), tags
        );
    }

    private final DecisionTreeSupQuestionHelper dtSupplementaryQuestionHelper = new DecisionTreeSupQuestionHelper(
            this,
            domainSolvingModel,
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
        return null; //FIXME удалить?
    }

    @Override
    public Explanation makeExplanation(List<ViolationEntity> mistakes, FeedbackType feedbackType, Language lang) {
        ArrayList<Explanation> result = new ArrayList<>();
        for (ViolationEntity mistake : mistakes) {
            result.add(new Explanation(Explanation.Type.ERROR, makeSingleExplanation(mistake, feedbackType, lang)));
        }
        return Explanation.aggregate(Explanation.Type.ERROR, result);
    }

    private HyperText makeSingleExplanation(ViolationEntity mistake, FeedbackType feedbackType, Language lang) {
        return new HyperText("WRONG");
    }

    private HashMap<String, Long> _getSkillsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(32);
        name2bit.put("central_operand_needed", 0x1L);  	// (1)
        name2bit.put("is_central_operand_evaluated", 0x2L);  	// (2)
        name2bit.put("nearest_operand_needed", 0x4L);  	// (4)
        name2bit.put("left_operand_needed", 0x8L);  	// (8)
        name2bit.put("right_operand_needed", 0x10L);  	// (16)
        name2bit.put("competing_operator_present", 0x20L);  	// (32)
        name2bit.put("left_competing_operator_present", 0x40L);  	// (64)
        name2bit.put("right_competing_operator_present", 0x80L);  	// (128)
        name2bit.put("current_operator_enclosed", 0x100L);  	// (256)
        name2bit.put("left_operator_enclosed", 0x200L);  	// (512)
        name2bit.put("right_operator_enclosed", 0x400L);  	// (1024)
        name2bit.put("order_determined_by_parentheses", 0x800L);  	// (2048)
        name2bit.put("is_current_parenthesized_left_not", 0x1000L);  	// (2^12)
        name2bit.put("is_current_parenthesized_right_not", 0x2000L);  	// (2^13)
        // (2^14) empty
        name2bit.put("is_left_parenthesized_current_not", 0x8000L);  	// (2^15)
        name2bit.put("is_right_parenthesized_current_not", 0x10000L);  	// (2^16)
        name2bit.put("order_determined_by_precedence", 0x20000L);  	// (2^17)
        name2bit.put("associativity_without_opposing_operand", 0x40000L);  	// (2^18)
        name2bit.put("associativity_without_left_opposing_operand", 0x80000L);  	// (2^19)
        name2bit.put("associativity_without_right_opposing_operand", 0x100000L);  	// (2^20)
        name2bit.put("order_determined_by_associativity", 0x200000L);  	// (2^21)
        name2bit.put("left_competing_to_right_associativity", 0x400000L);  	// (2^22)
        name2bit.put("right_competing_to_left_associativity", 0x800000L);  	// (2^23)
        name2bit.put("strict_order_operators_present", 0x1000000L);  	// (2^24)
        name2bit.put("strict_order_first_operand_to_be_evaluated", 0x2000000L);  	// (2^25)
        name2bit.put("is_first_operand_of_strict_order_operator_fully_evaluated", 0x4000000L);  	// (2^26)
        name2bit.put("no_omitted_operands_despite_strict_order", 0x8000000L);  	// (2^27)
        name2bit.put("should_strict_order_current_operand_be_omitted", 0x10000000L);  	// (2^28)
        name2bit.put("left_competing_to_right_precedence", 0x20000000L);
        name2bit.put("right_competing_to_left_precedence", 0x40000000L);
        name2bit.put("is_current_operator_strict_order", 0x80000000L);
        name2bit.put("are_central_operands_strict_order", 0x100000000L);
        name2bit.put("no_current_in_many_central_operands", 0x200000000L);
        name2bit.put("no_comma_in_central_operands", 0x400000000L);
        name2bit.put("previous_central_operands_are_unevaluated", 0x800000000L);
        name2bit.put("expression_strict_order_operators_present", 0x1000000000L);
        name2bit.put("earlyfinish_strict_order_operators_present", 0x2000000000L);
        name2bit.put("strict_order_first_operand_to_be_evaluated_while_solving", 0x4000000000L);
        name2bit.put("strict_order_first_operand_to_be_evaluated_while_earlyfinish", 0x8000000000L);
        name2bit.put("no_omitted_operands_despite_strict_order_while_solving", 0x10000000000L);
        name2bit.put("no_omitted_operands_despite_strict_order_while_earlyfinish", 0x20000000000L);
        name2bit.put("should_strict_order_current_operand_be_omitted_while_solving", 0x40000000000L);
        name2bit.put("should_strict_order_current_operand_be_omitted_while_earlyfinish", 0x80000000000L);
        name2bit.put("is_current_operator_strict_order_while_solving", 0x100000000000L);
        name2bit.put("is_current_operator_strict_order_while_earlyfinish", 0x200000000000L);
        return name2bit;
    }
}
