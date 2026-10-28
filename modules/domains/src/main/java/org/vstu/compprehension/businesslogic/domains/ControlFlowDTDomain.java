package org.vstu.compprehension.businesslogic.domains;

import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.data.question.BackendFactData;
import org.vstu.compprehension.businesslogic.domains.helpers.DomainSolvingModelLoader;
import its.model.DomainSolvingModel;
import its.model.definition.*;
import its.model.definition.rdf.DomainRDFFiller;
import its.model.definition.rdf.DomainRDFWriter;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import its.reasoner.nodes.DecisionTreeTrace;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.*;
import org.vstu.compprehension.services.RandomProvider;
import org.vstu.compprehension.businesslogic.SupplementaryStepContext;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.services.LocalizationService;
import org.vstu.compprehension.businesslogic.*;
import org.vstu.compprehension.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.businesslogic.backend.Fact;
import org.vstu.compprehension.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.businesslogic.backend.util.TermMapping;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.businesslogic.HyperText;

import java.util.*;
import java.util.stream.Collectors;
import org.vstu.compprehension.data.question.GeneratedQuestionData;
import org.vstu.compprehension.data.question.QuestionContentData;

@Log4j2
public class ControlFlowDTDomain extends DecisionTreeReasoningDomain {
    private final DecisionTreeInterface backendInterface = new DecisionTreeInterface();
    protected final LocalizationService localizationService;
    protected final QuestionBank qMetaStorage;

    static final String RESOURCES_LOCATION = "domains/";
    static final String MESSAGE_PREFIX = "ctrlflow_text.";
    public static final String DOMAIN_ID = "ctrl_flow_dt25";
    public static final int QUESTION_VERSION = 3;
    public static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "control-flow-domain-model/";
    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "control-flow";

    private final DomainSolvingModel domainSolvingModel = DomainSolvingModelLoader.loadFromClasspath(
            this.getClass().getClassLoader(),
            DOMAIN_MODEL_LOCATION,
            DomainSolvingModel.BuildMethod.LOQI).validate(false);

    private static final HashMap<String, Tag> tags = new HashMap<>() {{
        put("C++", new Tag("C++", 2L));  	// (2 ^ 1)
        put("Java", new Tag("Java", 4L));  	// (2 ^ 2)
        put("Python", new Tag("Python", 8L));  	// (2 ^ 3)
    }};

    static final String CHECK_TREE = "";
    static final String FIND_CORRECT_TREE = "findCorrect";
    private static final String RDF_BASE = TermMapping.BASE_URI_DEFAULT + "#";
    /** Объекты, которые рассуждение меняет по ходу решения: трасса, её состояние и расход значений. */
    private static final List<String> SOLUTION_CLASSES = List.of("TraceAct", "TraceState", "SemanticValue");

    public DomainModel prepareQuestionModel(QuestionContentData q) {
        return situationFromFacts(q.getStatementFacts());
    }

    /**
     * Ситуация вопроса с трассой засчитанных ответов. Начало трассы берётся из сохранённого решения,
     * если оно построено по началу этих ответов, остальные ответы заново проходят граф проверки.
     */
    DomainModel prepareSituationModel(QuestionContentData q, List<? extends AnswerData> accepted) {
        List<Integer> answerIds = collectAnswerIds(accepted);
        List<Integer> solvedIds = readSolutionAnswerIds(q.getSolutionFacts());
        boolean solutionFits = !solvedIds.isEmpty() && solvedIds.size() <= answerIds.size()
                && answerIds.subList(0, solvedIds.size()).equals(solvedIds);
        DomainModel situation = solutionFits
                ? situationFromFacts(withSolution(q.getStatementFacts(), q.getSolutionFacts()))
                : situationFromFacts(q.getStatementFacts());
        for (AnswerData answer : accepted.subList(solutionFits ? solvedIds.size() : 0, accepted.size())) {
            bindCheckVariables(situation, answer);
            DecisionTreeTrace trace = DecisionTreeReasoner.solve(domainSolvingModel.decisionTree(CHECK_TREE), learningSituation(situation));
            if (!DecisionTreeReasonerBackend.Interface.isCorrectAnswer(trace)) {
                throw new IllegalStateException("Accepted answer %s is rejected while restoring the trace of CtrlFlow question %s"
                        .formatted(answer.left().getDomainInfo(), q.getQuestionName()));
            }
        }
        return situation;
    }

    private DomainModel situationFromFacts(List<BackendFactData> facts) {
        DomainModel situation = domainSolvingModel.getDomainModel().copy();
        DomainRDFFiller.fillDomain(situation, JenaFactList.fromBackendFacts(facts).getModel(), Set.of(), null);
        localizeConditionValues(situation);
        return situation;
    }

    /** Значения условия подставляются в объяснения ошибок, а встроенное перечисление OptionalBool имён не несёт. */
    private void localizeConditionValues(DomainModel situation) {
        var values = situation.getEnums().get("OptionalBool").getValues();
        for (String value : List.of("true", "false")) {
            for (Language language : Language.values()) {
                values.get(value).getMetadata().add(language.toLocaleString(), "localizedName",
                        getMessage("trace.condition." + value, language));
            }
        }
    }

    /** Факты условия, в которых объекты решения заменены их состоянием из решения. */
    private static List<BackendFactData> withSolution(List<BackendFactData> statement, List<BackendFactData> solution) {
        List<BackendFactData> solutionObjects = solution.stream()
                .filter(fact -> !fact.getSubject().equals("solution"))
                .toList();
        Set<String> replaced = solutionObjects.stream().map(BackendFactData::getSubject).collect(Collectors.toSet());
        List<BackendFactData> facts = new ArrayList<>(statement.stream()
                .filter(fact -> !replaced.contains(fact.getSubject()))
                .toList());
        facts.addAll(solutionObjects);
        return facts;
    }

    /** Решение после засчитанных ответов: объекты решения ситуации целиком и answerId этих ответов. */
    private static List<BackendFactData> toSolutionFacts(DomainModel situation, List<? extends AnswerData> accepted) {
        // Входные переменные проверки относятся к одному шагу и в решение не входят.
        situation.getVariables().remove("P");
        situation.getVariables().remove("A");
        situation.getVariables().remove("V");
        Set<String> solutionObjects = situation.getObjects().stream()
                .filter(object -> SOLUTION_CLASSES.stream().anyMatch(object::isInstanceOf))
                .map(ObjectDef::getName)
                .collect(Collectors.toSet());
        List<BackendFactData> facts = new ArrayList<>(JenaBackend.modelToFacts(
                        DomainRDFWriter.saveDomain(situation, RDF_BASE, Set.of()), RDF_BASE).stream()
                .filter(fact -> solutionObjects.contains(fact.getSubject()))
                .toList());
        facts.add(new BackendFactData("", "solution", "acceptedAnswerIds", "xsd:string",
                collectAnswerIds(accepted).stream().map(String::valueOf).collect(Collectors.joining(","))));
        return facts;
    }

    /** Прочитать из сохранённого решения answerId ответов, по которым оно построено; пусто, если решения нет. */
    private static List<Integer> readSolutionAnswerIds(List<BackendFactData> solution) {
        return solution.stream()
                .filter(fact -> fact.getSubject().equals("solution") && fact.getVerb().equals("acceptedAnswerIds"))
                .findFirst()
                .filter(fact -> !fact.getObject().isEmpty())
                .map(fact -> Arrays.stream(fact.getObject().split(",")).map(Integer::valueOf).toList())
                .orElse(List.of());
    }

    /** answerId переданных ответов в том же порядке. */
    private static List<Integer> collectAnswerIds(List<? extends AnswerData> answers) {
        return answers.stream().map(answer -> answer.left().getAnswerId()).toList();
    }

    /** Входные переменные проверки ответа: текущая точка трассы, выбранное действие и выбранное с ним значение. */
    private static void bindCheckVariables(DomainModel situation, AnswerData answer) {
        String[] actionAndValue = answer.left().getDomainInfo().split(",", 2);
        bindVariable(situation, "P", traceTail(situation).getName());
        bindVariable(situation, "A", actionAndValue[0]);
        bindVariable(situation, "V", actionAndValue.length > 1 ? actionAndValue[1] : "NoValue");
    }

    private static void bindVariable(DomainModel situation, String name, String objectName) {
        situation.getVariables().remove(name);
        situation.getVariables().add(new VariableDef(name, objectName));
    }

    /** Последний акт трассы: конец цепочки directlyBeforeOf от act_root. */
    private static ObjectDef traceTail(DomainModel situation) {
        ObjectDef act = situation.getObjects().get("act_root");
        for (ObjectDef next = findNextAct(act); next != null; next = findNextAct(next)) {
            act = next;
        }
        return act;
    }

    private static @Nullable ObjectDef findNextAct(@NotNull ObjectDef act) {
        return findLinkedObject(act, "directlyBeforeOf");
    }

    private static @Nullable ObjectDef findActValue(@NotNull ObjectDef act) {
        return findLinkedObject(act, "hasValue");
    }

    private static @Nullable ObjectDef findLinkedObject(@NotNull ObjectDef object, @NotNull String relationship) {
        return object.getRelationshipLinks().stream()
                .filter(link -> link.getRelationshipName().equals(relationship))
                .findFirst()
                .map(link -> link.getObjects().getFirst())
                .orElse(null);
    }

    private LearningSituation learningSituation(DomainModel situation) {
        return new LearningSituation(situation, LearningSituation.collectDecisionTreeVariables(situation), domainSolvingModel);
    }

    /** Ответ на акт трассы: кнопка его действия либо пункт этого действия с выбранным значением. */
    private static AnswerObjectData findAnswerObject(QuestionContentData q, ObjectDef act) {
        String action = act.getRelationshipLink("hasAction").getObjects().getFirst().getName();
        Optional<String> withValue = Optional.ofNullable(findActValue(act)).map(value -> action + "," + value.getName());
        return q.getAnswerObjects().stream()
                .filter(answer -> withValue.map(answer.getDomainInfo()::equals).orElse(false))
                .findFirst()
                .or(() -> q.getAnswerObjects().stream().filter(answer -> answer.getDomainInfo().equals(action)).findFirst())
                .orElseThrow(() -> new IllegalStateException("No answer object for action " + action));
    }

    private static Map<String, Skill> buildSkills() {
        var b = new SkillsBuilder();
        var visible = EnumSet.of(DomainItemFlag.VISIBLE_TO_TEACHER);

        b.add("interruption_exits_constructs", 1L << 0, visible);
        b.add("condition_value_selects_next", 1L << 1, visible);
        b.add("construct_entered_before_inner", 1L << 2, visible);
        b.add("inner_construct_finished_before_next", 1L << 3, visible);
        b.add("passed_action_repeat", 1L << 4, visible);
        b.add("containing_action_before_inner", 1L << 5, visible);
        b.add("alternative_single_branch", 1L << 6, visible);
        b.add("alternative_conditions_in_order", 1L << 7, visible);
        b.add("actions_in_order", 1L << 8, visible);
        b.add("no_early_exit_without_interruption", 1L << 9, visible);
        // Метка завершения программы в графе, а не умение студента.
        b.add("everything_evaluated", 1L << 10);
        b.add("function_body_runs_on_call", 1L << 11, visible);
        b.add("function_not_resumed_after_exit", 1L << 12, visible);
        b.add("call_return_matches_activation", 1L << 13, visible);

        return b.build();
    }

    private static Map<String, Concept> buildConcepts() {
        var b = new ConceptsBuilder();

        var flags = EnumSet.of(DomainItemFlag.VISIBLE_TO_TEACHER, DomainItemFlag.TARGET_ENABLED);
        var invisible = EnumSet.of(DomainItemFlag.TARGET_ENABLED);

        Concept exprs = b.add("expressions");
        b.add("pointers", 1L << 0, List.of(exprs), flags);
        b.add("bitwise", 1L << 1, List.of(exprs), flags);
        b.add("member_access", 1L << 2, List.of(exprs), flags);
        b.add("arrays", 1L << 3, List.of(exprs), flags);
        b.add("type_casts", 1L << 4, List.of(exprs), flags);
        b.add("io", 1L << 5, List.of(exprs), flags);
        b.add("logical", 1L << 6, List.of(exprs), flags);
        b.add("arithmetic", 1L << 7, List.of(exprs), flags);
        b.add("ternary_conditions", 1L << 8, List.of(exprs), flags);
        b.add("lib_function_call", 1L << 9, List.of(exprs), flags);
        b.add("program_function_call", 1L << 10, List.of(exprs), flags);
        b.add("var_declaration", 1L << 11, List.of(exprs), flags);
        b.add("assignment", 1L << 12, List.of(exprs), flags);
        b.add("delete", 1L << 16, List.of(exprs), flags);
        b.add("strings", 1L << 17, List.of(exprs), flags);
        b.add("list_collections", 1L << 18, List.of(exprs), flags);
        b.add("map_collections", 1L << 19, List.of(exprs), flags);
        b.add("object_new", 1L << 20, List.of(exprs), flags);

        Concept units = b.add("units");
        b.add("function", 1L << 21, List.of(units), flags);
        b.add("structure", 1L << 22, List.of(units), flags);
        b.add("interface", 1L << 23, List.of(units), invisible);
        b.add("class", 1L << 24, List.of(units), invisible);
        b.add("field", 1L << 25, List.of(units), invisible);
        b.add("method", 1L << 26, List.of(units), invisible);
        b.add("constructor", 1L << 27, List.of(units), invisible);
        b.add("destructor", 1L << 28, List.of(units), invisible);
        b.add("generic_type", 1L << 29, List.of(units), invisible);
        b.add("return", 1L << 15, List.of(units), flags);

        Concept loops = b.add("loops");
        b.add("general_for_loop", 1L << 30, List.of(loops), flags);
        b.add("range_for_loop", 1L << 31, List.of(loops), flags);
        b.add("for_each_loop", 1L << 32, List.of(loops), flags);
        b.add("while_loop", 1L << 33, List.of(loops), flags);
        b.add("do_while_loop", 1L << 34, List.of(loops), flags);
        b.add("infinite_loop", 1L << 35, List.of(loops), flags);
        b.add("break", 1L << 13, List.of(loops), flags);
        b.add("continue", 1L << 14, List.of(loops), flags);

        Concept branches = b.add("branches");
        b.add("if", 1L << 36, List.of(branches), flags);
        b.add("else", 1L << 37, List.of(branches), flags);
        b.add("elseif", 1L << 38, List.of(branches), flags);
        b.add("switch", 1L << 39, List.of(branches), flags);
        b.add("fallthrough_case", 1L << 40, List.of(branches), flags);
        b.add("default_case", 1L << 41, List.of(branches), flags);

        b.add("recursion", 1L << 42, List.of(), flags);
        b.add("loop_iteration", 1L << 43, List.of(), flags);
        b.add("call_depth", 1L << 44, List.of(), flags);

        return b.build();
    }

    private class DecisionTreeInterface implements DecisionTreeReasonerBackend.Interface {

        @Override
        public DecisionTreeReasoningDomain getDomain() {
            return ControlFlowDTDomain.this;
        }

        @Override
        public InterpretSentenceResult interpretJudgeNotPerformed(QuestionData judgedQuestion, LearningSituation preparedSituation, Language language) {
            return null;
        }

        @Override
        public void updateJudgeInterpretationResult(InterpretSentenceResult interpretationResult, DecisionTreeReasonerBackend.Output backendOutput) {
            interpretationResult.CountCorrectOptions = 1;
        }

        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForJudge(QuestionData question, List<? extends AnswerData> responses, List<Tag> tags) {
            if (!Objects.equals(question.getContent().getMetadata().getVersion(), QUESTION_VERSION)) {
                throw new UnsupportedOperationException("Unsupported version of CtrlFlow question");
            }
            DomainModel situation = prepareSituationModel(question.getContent(), responses.subList(0, responses.size() - 1));
            bindCheckVariables(situation, responses.getLast());
            return new DecisionTreeReasonerBackend.Input(situation, domainSolvingModel.decisionTree(CHECK_TREE), domainSolvingModel);
        }

        @Override
        public List<BackendFactData> updateSolutionAfterJudge(QuestionData question, List<? extends AnswerData> responses,
                                                              DecisionTreeReasonerBackend.Output backendOutput) {
            return toSolutionFacts(backendOutput.situation().getDomainModel(), responses);
        }

        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForSolve(QuestionContentData question, List<Tag> tags) {
            return null;
        }
    }

    public ControlFlowDTDomain(RandomProvider randomProvider,
                               LocalizationService localizationService, QuestionBank qMetaStorage) {
        super(DOMAIN_ID, randomProvider, new DomainStructure(buildConcepts(), buildSkills(), Laws.empty()));
        this.qMetaStorage = qMetaStorage;
        this.localizationService = localizationService;
    }

    @Override
    public List<DomainSolvingModel> getDomainSolvingModels() {
        return List.of(domainSolvingModel);
    }

    @Override
    public DecisionTreeReasonerBackend.Interface getBackendInterface() {
        return backendInterface;
    }

    @NotNull
    @Override
    public String getDisplayName(Language language) {
        return getMessage("ctrlflow_text.display_name_dt", language);
    }

    @Nullable
    @Override
    public String getDescription(Language language) {
        return getMessage("ctrlflow_text.description", language);
    }

    @NotNull
    @Override
    public Map<String, Tag> getTags() {
        return tags;
    }

    @Override
    public Collection<Fact> responseToFacts(QuestionData question, List<? extends AnswerData> responses) {
        return List.of();
    }

    @Override
    public Collection<Fact> getQuestionStatementFactsWithSchema(QuestionContentData q) {
        return JenaFactList.fromBackendFacts(q.getStatementFacts());
    }

    @Override
    public Set<String> getViolationVerbs(String questionDomainType, List<BackendFactData> statementFacts) {
        return Set.of();
    }

    @Override
    public Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactData> statementFacts) {
        return Set.of();
    }

    @Override
    public Collection<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        return List.of();
    }

    @Override
    public Collection<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        return List.of();
    }

    @Override
    public InterpretSentenceResult interpretSentence(Collection<Fact> violations) {
        return null; // не нужно для DT
    }

    @Override
    public Explanation makeExplanation(List<ViolationData> mistakes, FeedbackType feedbackType, Language lang) {
        ArrayList<Explanation> result = new ArrayList<>();
        for (ViolationData mistake : mistakes) {
            result.add(new Explanation(Explanation.Type.ERROR, makeSingleExplanation(mistake, feedbackType, lang)));
        }
        return Explanation.aggregate(Explanation.Type.ERROR, result);
    }

    private HyperText makeSingleExplanation(ViolationData mistake, FeedbackType feedbackType, Language lang) {
        return new HyperText("WRONG");
    }


    @NotNull
    @Override
    public GeneratedQuestionData makeQuestion(@NotNull QuestionRequest questionRequest,
                                              @Nullable ExerciseOptionsData exerciseOptions,
                                              @NotNull Language userLanguage
    ) {
        HashSet<String> conceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getTargetConcepts()) {
            conceptNames.add(concept.getName());
        }

        List<QuestionMetadataWithData> foundQuestions = null;

        try {
            int generatorThreshold = exerciseOptions.getGeneratorThreshold() != null
                    ? exerciseOptions.getGeneratorThreshold()
                    : (int)(exerciseOptions.getMaxExpectedConcurrentStudents() * 1.5);
            int generatorAdditionalQuestionsToGenerate = exerciseOptions.getGeneratorAdditionalQuestionsToGenerate() != null
                    ? exerciseOptions.getGeneratorAdditionalQuestionsToGenerate()
                    : 3;
            foundQuestions = qMetaStorage.searchQuestions(questionRequest, 1, generatorThreshold, generatorAdditionalQuestionsToGenerate).getQuestions();

            // search again if nothing found with "TO_COMPLEX"
            SearchDirections lawsSearchDir = questionRequest.getLawsSearchDirection();
            if (foundQuestions.isEmpty() && lawsSearchDir == SearchDirections.TO_COMPLEX) {
                questionRequest = questionRequest.toBuilder().lawsSearchDirection(SearchDirections.TO_SIMPLE).build();
                foundQuestions = qMetaStorage.searchQuestions(questionRequest, 1, generatorThreshold, generatorAdditionalQuestionsToGenerate).getQuestions();
            }
        } catch (Exception e) {
            // file storage was not configured properly...
            log.error("Error searching questions - {}", e.getMessage(), e);
            foundQuestions = new ArrayList<>();
        }


        if (foundQuestions == null || foundQuestions.isEmpty()) {
            throw new IllegalStateException("No valid questions found");
        }

        var res = foundQuestions.getFirst();
        return makeQuestion(res, List.of(), userLanguage);
    }

    @NotNull
    @Override
    public GeneratedQuestionData makeQuestion(@NotNull QuestionMetadataWithData metadata,
                                              @NotNull List<Tag> tags, @NotNull Language userLang
    ) {
        if (!Objects.equals(metadata.getVersion(), QUESTION_VERSION)) {
            throw new IllegalArgumentException("Unsupported version %s of CtrlFlow question %s".formatted(metadata.getVersion(), metadata.getName()));
        }
        var result = metadata.getData().toQuestion(this, metadata);
        return result.withContent(result.getContent().toBuilder()
                .questionText(getMessage("question_prompt", userLang).concat(result.getContent().getQuestionText()))
                .build());
    }

    @Override
    public SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionData sourceQuestion, @Nullable SupplementaryStepData latestStep, ViolationData violation, Language lang) {
        return null;
    }

    @Override
    public QuestionRequest ensureQuestionRequestValid(QuestionRequest questionRequest) {
        return questionRequest.toBuilder()
                .stepsMin(2)
                .stepsMax(23)
                .questionVersion(QUESTION_VERSION)
                .build();
    }

    @Override
    public SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(QuestionData mainQuestion, SupplementaryStepContext step, List<? extends AnswerData> responses, Language language) {
        return null;
    }

    @Override
    public boolean needSupplementaryQuestion(String violationLawName, InteractionType interactionType) {
        return false;
    }

    @Override
    public InterpretSentenceResult judgeQuestion(QuestionData question, List<? extends AnswerData> responses, List<Tag> tags, Language language) {
        var result = super.judgeQuestion(question, responses, tags, language);
        // Корректная трасса задачи единственна: осталось столько шагов решения, сколько ещё не засчитано.
        int accepted = result.isAnswerCorrect ? responses.size() : responses.size() - 1;
        result.IterationsLeft = Math.max(0, question.getContent().getMetadata().getSolutionSteps() - accepted);
        return result;
    }

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(QuestionData q, Language language) {
        DomainModel situation = prepareSituationModel(q.getContent(), q.findLatestCorrectAnswers());
        bindVariable(situation, "P", traceTail(situation).getName());
        situation.getVariables().remove("A");
        situation.getVariables().remove("V");
        var learningSituation = learningSituation(situation);
        DecisionTreeTrace trace = DecisionTreeReasoner.solve(domainSolvingModel.decisionTree(FIND_CORRECT_TREE), learningSituation);
        if (!DecisionTreeReasonerBackend.Interface.isCorrectAnswer(trace)) {
            throw new IllegalStateException("No next correct action for CtrlFlow question " + q.getContent().getQuestionName());
        }
        // Найденный шаг студента становится новым P; после него граф мог добавить прозрачный вход в конструкцию.
        ObjectDef step = learningSituation.getDecisionTreeVariables().get("P").findIn(situation);
        AnswerObjectData answer = findAnswerObject(q.getContent(), step);

        CorrectAnswer correctAnswer = new CorrectAnswer();
        correctAnswer.answers = List.of(new AnswerData.Pair(answer, answer));
        correctAnswer.question = q;
        correctAnswer.lawName = null;
        correctAnswer.skillName = List.of();
        correctAnswer.explanation = DecisionTreeReasonerBackend.collectExplanationsFromTrace(
                Explanation.Type.HINT, trace, situation, this, language);
        return correctAnswer;
    }

    private static String replaceInString(String s, Map<String, String> placeholders) {
        // Build StringSubstitutor
        StringSubstitutor stringSubstitutor = new StringSubstitutor(placeholders);
        stringSubstitutor.setEnableUndefinedVariableException(true);

        // Replace in message
        try {
            return stringSubstitutor.replace(s);
        }
        catch (IllegalArgumentException exception) {
            return exception.getMessage() + " — template: " + s + " — placeholders: " + (placeholders.entrySet().stream()).map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
        }
    }

    private static String formatNthTime(int n, Language lang) {
        if (lang.equals(Language.ENGLISH)) {
            if (n % 100 >= 11 && n % 100 <= 13) {
                return n + "th";
            }
            return switch (n % 10) {
                case 1 -> n + "st";
                case 2 -> n + "nd";
                case 3 -> n + "rd";
                default -> n + "th";
            };
        } else if (lang.equals(Language.RUSSIAN)) {
            return String.format("%d-й", n);
        } else {
            return Integer.toString(n);
        }
    }

    private static String htmlStyleFormat(String text, String style) {
        return "<span class=\"%s\">%s</span>".formatted(style, text);
    }

    private String getMessageWithSuffix(String rawName, String suffix, Language lang) {
        var rawNameFilled = rawName.concat(suffix);
        var message = getMessage(rawNameFilled, lang);
        if (message == null || message.isEmpty() || message.equals(rawNameFilled)) {
            message = getMessage(rawName, lang);
        }
        return message;
    }

    @NotNull
    @Override
    public List<HyperText> getFullSolutionTrace(@NotNull QuestionData question, @NotNull Language language) {
        DomainModel situation = prepareSituationModel(question.getContent(), question.findLatestCorrectAnswers());
        Set<String> answeredActions = question.getContent().getAnswerObjects().stream()
                .map(answer -> answer.getDomainInfo().split(",", 2)[0])
                .collect(Collectors.toSet());
        Map<String, Integer> repeats = new HashMap<>();
        List<HyperText> trace = new ArrayList<>();
        for (ObjectDef act = situation.getObjects().get("act_root"); act != null; act = findNextAct(act)) {
            ObjectDef action = act.getRelationshipLink("hasAction").getObjects().getFirst();
            // Действия без кнопки проходятся автоматически: прозрачные и непрозрачные действия,
            // раскрывающиеся в составную конструкцию (тело функции, ветвь, первый оператор блока).
            if (!answeredActions.contains(action.getName())) {
                continue;
            }
            String line = formatTraceLine(action, findActValue(act), repeats.merge(action.getName(), 1, Integer::sum), language);
            trace.add(new HyperText("<span class=\"trace-act\" data-action=\"%s\">%s</span>".formatted(action.getName(), line)));
        }
        // Ошибочный ответ в трассу не вошёл: его строка не помечается актом трассы.
        latestMistakenAnswer(question).ifPresent(answer -> {
            String[] actionAndValue = answer.left().getDomainInfo().split(",", 2);
            ObjectDef action = situation.getObjects().get(actionAndValue[0]);
            ObjectDef value = actionAndValue.length > 1 ? situation.getObjects().get(actionAndValue[1]) : null;
            int repeat = repeats.getOrDefault(action.getName(), 0) + 1;
            trace.add(new HyperText(htmlStyleFormat(formatTraceLine(action, value, repeat, language), "warning")));
        });
        return trace;
    }

    private static @NotNull Optional<AnswerData> latestMistakenAnswer(@NotNull QuestionData question) {
        List<QuestionInteractionData> interactions = question.getInteractions();
        if (interactions.isEmpty() || interactions.getLast().isCorrect() || interactions.getLast().getAnswers().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(interactions.getLast().getAnswers().getLast());
    }

    /** BEGIN и END называют свою конструкцию, остальные действия — себя. */
    private @NotNull String formatTraceLine(@NotNull ObjectDef action, @Nullable ObjectDef value, int repeat, @NotNull Language language) {
        Object role = actionSpec(action).getPropertyValue("role", Map.of());
        boolean begins = "BEGIN".equals(role);
        boolean ends = "END".equals(role);
        ObjectDef named = begins || ends ? action.getRelationshipLink("belongsTo").getObjects().getFirst() : action;
        String state = begins ? "trace.began" : ends ? "trace.ended" : "trace.executed";
        String pronoun = named.getMetadata().getString("pronoun");
        return replaceInString(getMessage("trace.template", language), Map.of(
                "structure", htmlStyleFormat(named.getMetadata().getString(language.toLocaleString(), "localizedName"), "action"),
                "action_state", htmlStyleFormat(getMessageWithSuffix(state, pronoun == null ? "" : "." + pronoun, language), "keyword"),
                "nth_time", htmlStyleFormat(formatNthTime(repeat, language), "number"),
                "value", formatTraceValue(value, language)));
    }

    private static @NotNull ObjectDef actionSpec(@NotNull ObjectDef action) {
        return action.getRelationshipLink("derivedFrom").getObjects().getFirst();
    }

    /** Значение после тире: его имя, без имени — подсказка, без обоих — ничего. */
    private static @NotNull String formatTraceValue(@Nullable ObjectDef value, @NotNull Language language) {
        if (value == null) {
            return "";
        }
        String text = value.getMetadata().getString(language.toLocaleString(), "localizedName");
        if (text == null || text.isEmpty()) {
            text = value.getMetadata().getString("hint");
        }
        if (text == null || text.isEmpty()) {
            return "";
        }
        return " — " + htmlStyleFormat(StringEscapeUtils.escapeHtml4(text), "value-hint");
    }

    @Override
    public String getMessage(String base_question_text, Language preferredLanguage) {
        String key = base_question_text;
        if (!base_question_text.startsWith(MESSAGE_PREFIX)) {
            key = MESSAGE_PREFIX + base_question_text;
        }
        var found = localizationService.getMessage(key, Language.getLocale(preferredLanguage));
        if (found.equals(key))
            return base_question_text;
        return found;
    }

    @Override
    protected List<GeneratedQuestionData> getQuestionTemplates() {
        return List.of();
    }
}
