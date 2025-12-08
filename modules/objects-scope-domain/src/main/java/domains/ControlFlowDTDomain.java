package domains;

import its.model.DomainSolvingModel;
import its.model.definition.*;
import its.model.definition.build.DomainBuilderUtils;
import its.model.definition.loqi.DomainLoqiBuilder;
import its.model.nodes.BranchResult;
import its.model.nodes.DecisionTree;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import its.reasoner.nodes.DecisionTreeTrace;
import its.reasoner.nodes.DecisionTreeTraceElement;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.dto.ExerciseSkillDto;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.domains.DecisionTreeReasoningDomain;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.io.StringReader;
import java.util.*;

@Log4j2
public class ControlFlowDTDomain extends DecisionTreeReasoningDomain {
    @Getter private final DecisionTreeInterface backendInterface = new DecisionTreeInterface();
    protected final LocalizationService localizationService;
    protected final QuestionBank qMetaStorage;

    static final String RESOURCES_LOCATION = "domains/";
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String MESSAGE_PREFIX = "ctrlflow_text.";
    static final String DOMAIN_SHORTNAME = "ctrl_flow_dt25";
    public static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "control-flow-domain-model/";
    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "control-flow";

    private final DomainSolvingModel domainSolvingModel = new DomainSolvingModel(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION)),
            DomainSolvingModel.BuildMethod.LOQI).validate();

    private static final HashMap<String, Tag> tags = new HashMap<>() {{
        put("C++", new Tag("C++", 2L));  	// (2 ^ 1)
        put("Java", new Tag("Java", 4L));  	// (2 ^ 2)
        put("Python", new Tag("Python", 8L));  	// (2 ^ 3)
    }};

    public DomainModel prepareQuestionModel(Question q, DomainSolvingModel domainSolvingModel) {
        var loqiText = q.getStatementFacts().getFirst().getObject();
        DomainModel commonModel = domainSolvingModel.getDomainModel();
        DomainModel resultModel = commonModel.copy();
        DomainModel situationModel;
        try (StringReader reader = new StringReader(loqiText)) {
            situationModel = DomainLoqiBuilder.buildDomain(reader);
        }
        resultModel.addMerge(situationModel);
        return resultModel;
    }

    private void fillSkills() {
        skills = new HashMap<>();

        addSkill("compound_constructs_present", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("is_in_compound_construct_ending", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("is_in_nested_construct", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("selected_transition_without_any_constraint", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("interruption_state_matches_constraint", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("condition_value_allows_transition", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("is_function_call_jumping_correct", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("two_execution_points_has_path", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("are_condition_evaluation_required_in_path", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("many_actions_in_require_selection", Skill.FLAG_VISIBLE_TO_TEACHER);

        fillSkillTree();

        // assign mask bits to Skills
        var name2bit = _getSkillsName2bit();
        for (Skill t : skills.values()) {
            var name = t.getName();
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

    private static class DecisionTreeInterface implements DecisionTreeReasonerBackend.Interface {

        @Override
        public InterpretSentenceResult interpretJudgeNotPerformed(Question judgedQuestion, LearningSituation preparedSituation) {
            return null;
        }

        ObjectDef findEndOfProgram(DomainModel model) {
            return model.getObjects().stream()
                    .filter(e -> e.getClassName().equals("Node"))
                    .filter(e -> ((EnumValueRef) e.getPropertyValue("kind", Map.of())).getValueName().equals("END")
                            && e.getRelationshipLink("hasMetadata")
                            .getObjects().getFirst()
                            .getRelationshipLink("belongsToASTNode")
                            .getObjects().getFirst().getPropertyValue("ast_node", Map.of()).equals("program_entry_point")
                    ).findFirst().get();
        }

        @Override
        public void updateJudgeInterpretationResult(InterpretSentenceResult interpretationResult, DecisionTreeReasonerBackend.Output backendOutput) {
            ObjectDef A_node = backendOutput.situation().getDecisionTreeVariables().get("A")
                    .findIn(backendOutput.situation().getDomainModel())
                    .getRelationshipLink("hasCFGNode").getObjects().getFirst();
            ObjectDef endOfProgram = findEndOfProgram(backendOutput.situation().getDomainModel());

            interpretationResult.CountCorrectOptions = 1;
            interpretationResult.IterationsLeft = (Integer) backendOutput.situation().getDomainModel()
                    .getObjects().stream().filter(obj -> obj.getClassName().equals("PathInfo")).filter(
                            path -> path.getRelationshipLink("from_").getObjects().getFirst().equals(A_node) &&
                                    path.getRelationshipLink("to_").getObjects().getFirst().equals(endOfProgram)
                    ).findFirst().orElseThrow().getPropertyValue("opaque_actions", Map.of());

            if (interpretationResult.IterationsLeft == 0) {
                // Достигли полного завершения задачи.
                // Ошибок уже быть не может — сбросим их все.
                interpretationResult.isAnswerCorrect = true;
                interpretationResult.violations = List.of();
                interpretationResult.explanation = Explanation.empty(Explanation.Type.HINT);
            }
        }

        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForJudge(Question question, List<ResponseEntity> responses, List<Tag> tags) {
            if (question.getMetadata().getVersion() != 2) {
                throw new UnsupportedOperationException("Unsupported version of CtrlFlow question");
            }
            var domain = (ControlFlowDTDomain) question.getDomain();
            var model = domain.getDomainSolvingModels().getFirst();

            DomainModel questionModel = domain.prepareQuestionModel(question, model);

            // Строим трассу, попутно проверяя ее с теневой эталонной трассой
            ObjectDef currentTraceAct = makeTrace(questionModel, responses, false);

            ObjectDef A = makeA(questionModel, responses.getLast().getLeftAnswerObject().getDomainInfo());
            ObjectDef L0 = currentTraceAct;

            updateModelState(domain, questionModel, L0, A);

            return new DecisionTreeReasonerBackend.Input(questionModel, model.getDecisionTree());
        }

        void updateModelState(ControlFlowDTDomain domain, DomainModel questionModel, ObjectDef L0, ObjectDef A) {
            ObjectDef STATE = questionModel.getVariables().get("STATE").getValueObject();

            ObjectDef pathL0A = domain.findPathInfo(questionModel,
                    L0.getRelationshipLink("hasCFGNode").getObjects().getFirst(),
                    A.getRelationshipLink("hasCFGNode").getObjects().getFirst()
            ).orElseThrow();

            // Обновляем состояние прерывания, исходя из маршрута от L0 до A
            if (pathL0A.getRelationshipLinks().stream().anyMatch(rel -> rel.getRelationshipName().equals("hasEffects"))) {
                var intrptStart = pathL0A.getRelationshipLink("hasEffects")
                        .getObjects().getFirst()
                        .getPropertyValue("interruption_start", Map.of());
                var interptStop = pathL0A.getRelationshipLink("hasEffects")
                        .getObjects().getFirst()
                        .getPropertyValue("interruption_stop", Map.of());
                var oldInterpt = STATE.getDefinedPropertyValues().get("interruption_state", Map.of());
                var noInterrupt = questionModel.getEnums().get("InterruptionType").getValues().get("no_intteruption").getReference();
                if (interptStop.equals(oldInterpt)) {
                    STATE.getDefinedPropertyValues().addOrReplace(
                            new PropertyValueStatement<>(A, "interruption_state", ParamsValues.getEMPTY(), noInterrupt)
                    );
                }
                if (!intrptStart.equals(oldInterpt)) {
                    STATE.getDefinedPropertyValues().addOrReplace(
                            new PropertyValueStatement<>(A, "interruption_state", ParamsValues.getEMPTY(), intrptStart)
                    );
                }
            }

            // Выставляем итоговые переменные
            questionModel.getVariables().add(new VariableDef("A", A.getName()));
            questionModel.getVariables().add(new VariableDef("L0", L0.getName()));
        }

        ObjectDef makeTrace(DomainModel questionModel, List<ResponseEntity> responses, boolean includeLast) {
            ObjectDef firstTraceAct = questionModel.getObjects()
                    .stream()
                    .filter(obj -> obj.getClassName().equals("TraceAct"))
                    .filter(obj -> (Boolean) obj.getPropertyValue("is_known_correct", Map.of()))
                    .findFirst().orElseThrow();

            ObjectDef currentTraceAct = firstTraceAct.getRelationshipLink("directlyBeforeOf").getObjects().getFirst();
            int end = includeLast ? responses.size() : responses.size() - 1;
            if (end == 0) currentTraceAct = firstTraceAct;
            for (int i = 0; i < end; i++) {
                ResponseEntity response = responses.get(i);
                String domainInfo = response.getLeftAnswerObject().getDomainInfo();
                ObjectDef cfgNode = questionModel.getObjects().stream()
                        .filter(obj -> obj.getClassName().equals("Node"))
                        .filter(obj -> obj.getPropertyValue("id", Map.of()).equals(domainInfo))
                        .findFirst().orElseThrow();
                if (currentTraceAct.getRelationshipLink("hasCFGNode")
                        .getObjects().getFirst().equals(cfgNode)) {
                    currentTraceAct.getDefinedPropertyValues().addOrReplace(new PropertyValueStatement<>(
                            currentTraceAct,
                            "is_known_correct",
                            ParamsValues.getEMPTY(), true));
                } else {
                    throw new DomainUseException("Invalid trace act in already checked acts");
                }
                if ((i + 1) != end) {
                    currentTraceAct = currentTraceAct.getRelationshipLink("directlyBeforeOf").getObjects().getFirst();
                }
            }
            return currentTraceAct;
        }

        ObjectDef makeA(DomainModel questionModel, String cfgNodeId) {
            ObjectDef result = DomainBuilderUtils.newObject(questionModel, "trace_act_A", "TraceAct");
            ObjectDef cfgNode = questionModel.getObjects().stream()
                    .filter(obj -> obj.getClassName().equals("Node"))
                    .filter(obj -> obj.getPropertyValue("id", Map.of()).equals(cfgNodeId))
                    .findFirst().orElseThrow();
            ObjectDef metadata = cfgNode.getRelationshipLink("hasMetadata").getObjects().getFirst();
            EnumValueRef noValue = questionModel.getEnums().get("OptionalBoolValue").getValues().get("no_value").getReference();
            result.getDefinedPropertyValues().addOrReplace(new PropertyValueStatement<>(result, "is_known_correct", ParamsValues.getEMPTY(), false));
            result.getDefinedPropertyValues().addOrReplace(new PropertyValueStatement<>(result, "condition_value", ParamsValues.getEMPTY(), noValue));

            result.getRelationshipLinks().add(new RelationshipLinkStatement(result, "hasCFGNode", List.of(cfgNode.getName()), ParamsValues.getEMPTY()));
            result.getRelationshipLinks().add(new RelationshipLinkStatement(result, "hasActionSpec",
                    List.of(metadata.getRelationshipLink("hasAbstractAction").getObjects().getFirst().getName()),
                    ParamsValues.getEMPTY()));
            result.getRelationshipLinks().add(new RelationshipLinkStatement(result, "hasASTNode",
                    List.of(metadata.getRelationshipLink("belongsToASTNode").getObjects().getFirst().getName()),
                    ParamsValues.getEMPTY()));
            return result;
        }

        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForSolve(Question question, List<Tag> tags) {
            return null;
        }
    }

    @Nullable
    public Optional<ObjectDef> findPathInfo(DomainModel questionModel, ObjectDef from, ObjectDef to) {
        return questionModel.getObjects().stream()
                .filter(obj -> obj.getClassName().equals("PathInfo"))
                .filter(pathInfo ->
                        pathInfo.getRelationshipLink("from_").getObjects().getFirst().equals(from)
                                && pathInfo.getRelationshipLink("to_").getObjects().getFirst().equals(to)).findFirst();
    }

    public ControlFlowDTDomain(DomainEntity domainEntity,
                               RandomProvider randomProvider,
                               LocalizationService localizationService, QuestionBank qMetaStorage) {
        super(domainEntity, randomProvider);
        this.qMetaStorage = qMetaStorage;
        this.localizationService = localizationService;
        this.concepts = Map.of();
        this.positiveLaws = Map.of();
        this.negativeLaws = Map.of();
        fillSkills();
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
        return getMessage("ctrlflow_text.display_name", language);
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
    public Collection<Fact> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            List<Fact> result = new ArrayList<>();
            for (ResponseEntity response : responses) {
                result.add(new Fact(
                        "owl:NamedIndividual",
                        response.getLeftAnswerObject().getDomainInfo(),
                        "hasAnswer",
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
        return JenaFactList.fromBackendFacts(q.getQuestionData().getStatementFacts());
    }

    @Override
    public Set<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return Set.of();
    }

    @Override
    public Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
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

    @NotNull
    @Override
    public String getShortName() {
        return DOMAIN_SHORTNAME;
    }

    @Override
    public InterpretSentenceResult interpretSentence(Collection<Fact> violations) {
        return null; // не нужно для DT
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


    @NotNull
    @Override
    public Question makeQuestion(@NotNull QuestionRequest questionRequest,
                                 @NotNull ExerciseAttemptEntity exerciseAttempt,
                                 @NotNull Language userLanguage
    ) {
        HashSet<String> conceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getTargetConcepts()) {
            conceptNames.add(concept.getName());
        }

        List<QuestionMetadataEntity> foundQuestions = null;

        try {
            int generatorThreshold = (int)(exerciseAttempt.getExercise().getOptions().getMaxExpectedConcurrentStudents() * 1.5);
            foundQuestions = qMetaStorage.searchQuestions(questionRequest, 1, generatorThreshold).getQuestions();

            // search again if nothing found with "TO_COMPLEX"
            SearchDirections lawsSearchDir = questionRequest.getLawsSearchDirection();
            if (foundQuestions.isEmpty() && lawsSearchDir == SearchDirections.TO_COMPLEX) {
                questionRequest.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
                foundQuestions = qMetaStorage.searchQuestions(questionRequest, 1, generatorThreshold).getQuestions();
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
        return makeQuestion(res, exerciseAttempt, List.of(), userLanguage);
    }

    @NotNull
    @Override
    public Question makeQuestion(@NotNull QuestionMetadataEntity metadata,
                                 @Nullable ExerciseAttemptEntity exerciseAttemptEntity,
                                 @NotNull List<Tag> tags, @NotNull Language userLang
    ) {
        var result = metadata.getQuestionData().getData().toQuestion(this, metadata);
        result.getQuestionData().setQuestionText(getMessage("question_prompt", userLang)
                .concat(result.getQuestionData().getQuestionText()));
        return result;
    }

    @Override
    public SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionEntity sourceQuestion, ViolationEntity violation, Language lang) {
        return null;
    }

    @Override
    public QuestionRequest ensureQuestionRequestValid(QuestionRequest questionRequest) {
        return questionRequest.toBuilder()
                .stepsMin(2)
                .stepsMax(23)
                .build();
    }

    @Override
    public SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(Question question, SupplementaryStepEntity supplementaryStep, List<ResponseEntity> responses) {
        return null;
    }

    @Override
    public boolean needSupplementaryQuestion(ViolationEntity violation) {
        return false;
    }

    private static class Solver {
        record SolveResult(boolean solved,
                                  List<String> laws,
                                  List<String> skills,
                                  DecisionTreeTrace trace, LearningSituation situation) {}

        private static void collectMeta(DecisionTreeTrace trace, List<String> skills, List<String> laws) {
            for (DecisionTreeTraceElement<?, ?> res : trace) {
                String[] resSkill = res.getNode().getMetadata().containsAny("skill") && res.getNode().getMetadata().get("skill") != null ?
                        res.getNode().getMetadata().get("skill").toString().split(";") : new String[0];
                String[] resLaw = res.getNode().getMetadata().containsAny("law") && res.getNode().getMetadata().get("law") != null ?
                        res.getNode().getMetadata().get("law").toString().split(";") : new String[0];
                Collections.addAll(skills, resSkill);
                Collections.addAll(laws, resLaw);
                for (var childTrace : Objects.requireNonNullElse(res.nestedTraces(), new ArrayList<DecisionTreeTrace>())) {
                    collectMeta(childTrace, skills, laws);
                }
            }
        }

        static SolveResult solve(DecisionTree tree, DomainModel model) {
            LearningSituation situation = new LearningSituation(model, new HashMap<>());
            DecisionTreeTrace trace = DecisionTreeReasoner.solve(tree, situation);
            List<String> skills = new ArrayList<>();
            List<String> laws = new ArrayList<>();
            boolean solved = trace.getBranchResult().equals(BranchResult.CORRECT);
            collectMeta(trace, skills, laws);
            return new SolveResult(solved, laws, skills, trace, situation);
        }
    }

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        Language lang = Optional.ofNullable(q.getQuestionData().getExerciseAttempt())
                .map(a -> a.getUser().getPreferred_language())
                .orElse(Language.RUSSIAN/*ENGLISH*/);
        List<String> deniedSkills = List.of();
        var exerciseStage = q.getExerciseStage();
        if (exerciseStage.isPresent()) {
            deniedSkills = exerciseStage.get().getSkills()
                    .stream().map(ExerciseSkillDto::getName).toList();
        }
        Optional<InteractionEntity> lastCorrectInteraction = Optional.ofNullable(q.getQuestionData().getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().isEmpty())
                .reduce((first, second) -> second);
        List<ResponseEntity> responses = new ArrayList<>();
        lastCorrectInteraction.ifPresent(interactionEntity -> responses.addAll(interactionEntity.getResponses()));
        var model = getDomainSolvingModels().getFirst();
        ControlFlowDTDomain.DecisionTreeInterface treeInterface = (ControlFlowDTDomain.DecisionTreeInterface) getBackendInterface();
        DomainModel questionModel = prepareQuestionModel(q, model);
        ObjectDef L0 = treeInterface.makeTrace(questionModel, responses, true);

        ObjectDef nextAnswer = L0.getRelationshipLink("directlyBeforeOf").getObjects().getFirst();
        ObjectDef endOfProgram = treeInterface.findEndOfProgram(questionModel);
        CorrectAnswer correctAnswer = new CorrectAnswer();
        String cfgId = (String) nextAnswer.getRelationshipLink("hasCFGNode")
                .getObjects().getFirst().getPropertyValue("id", Map.of());
        ObjectDef A = treeInterface.makeA(questionModel, cfgId);
        treeInterface.updateModelState(this, questionModel, L0, A);
        var solveRes = Solver.solve(ControlFlowDTDomain.this.getDomainSolvingModels().getFirst().getDecisionTree(), questionModel);
        Explanation explanation = DecisionTreeReasonerBackend.collectExplanationsFromTrace(
                Explanation.Type.HINT,
                solveRes.trace(), questionModel,
                this, deniedSkills, lang
        );
        AnswerObjectEntity answer = q.getAnswerObjects().stream().filter(ans -> ans.getDomainInfo().equals(cfgId)).findFirst().get();
        correctAnswer.answers = List.of(new CorrectAnswer.Response(answer, answer));
        correctAnswer.question = q.getQuestionData();
        correctAnswer.lawName = null;
        correctAnswer.skillName = solveRes.skills();
        correctAnswer.explanation = explanation;
        return correctAnswer;
    }

    @Override
    public List<HyperText> getFullSolutionTrace(Question question) {
        List<HyperText> trace = new ArrayList<>();
        var questionModel = prepareQuestionModel(question, this.domainSolvingModel);
        for (var object : questionModel.getObjects()
                .stream()
                .filter(obj -> obj.getClassName().equals("TraceAct")).toList()) {
            if ((Boolean) object.getPropertyValue("active", Map.of())) {
                var node = object.getRelationshipLinks().stream().filter(x ->
                        x.getRelationship().getName().equals("hasCFGNode")).findFirst().orElseThrow().getObjects().getFirst();
                var astNode = object.getRelationshipLinks().stream().filter(x ->
                        x.getRelationship().getName().equals("hasASTNode")).findFirst().orElseThrow().getObjects().getFirst();
                var action = object.getRelationshipLinks().stream().filter(x ->
                        x.getRelationship().getName().equals("hasActionSpec")).findFirst().orElseThrow().getObjects().getFirst();
                var nodeKind = node.getPropertyValue("kind", Map.of());
                var actionKind = action.getPropertyValue("kind", Map.of());
                trace.add(new HyperText("{} {}".formatted(nodeKind, actionKind)));
            }
        }
        return trace;
    }

    private HashMap<String, Long> _getSkillsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(16);
        name2bit.put("compound_constructs_present", 0x1L);   // (1)
        name2bit.put("is_in_compound_construct_ending", 0x2L);     // (2)
        name2bit.put("is_in_nested_construct", 0x4L);   // (4)
        name2bit.put("selected_transition_without_any_constraint", 0x8L);      // (8)
        name2bit.put("interruption_state_matches_constraint", 0x10L);    // (16)
        name2bit.put("condition_value_allows_transition", 0x20L);      // (32)
        name2bit.put("is_function_call_jumping_correct", 0x40L);     // (64)
        name2bit.put("two_execution_points_has_path", 0x80L);    // (128)
        name2bit.put("are_condition_evaluation_required_in_path", 0x100L);      // (256)
        name2bit.put("many_actions_in_require_selection", 0x200L);     // (512)
        return name2bit;
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
    protected List<Question> getQuestionTemplates() {
        return List.of();
    }
}
