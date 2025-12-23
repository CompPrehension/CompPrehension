package domains;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.val;
import org.apache.commons.text.StringSubstitutor;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

        ObjectDef findStartOfProgram(DomainModel model) {
            return model.getObjects()
                    .stream()
                    .filter(obj -> obj.getClassName().equals("TraceAct"))
                    .filter(obj -> (Boolean) obj.getPropertyValue("is_known_correct", Map.of()))
                    .findFirst().orElseThrow();
        }

        int calculateInteractionsLeftByTrace(ObjectDef currentTraceAct) {
            int count = 0;
            ObjectDef act = currentTraceAct;
            while (act.getRelationshipLinks().stream()
                    .anyMatch(rel -> rel.getRelationshipName().equals("directlyBeforeOf"))) {
                act = act.getRelationshipLink("directlyBeforeOf").getObjects().getFirst();
                count++;
            }
            return count;
        }

        int calculateInteractionsLeftByPath(DomainModel model, ObjectDef lastCorrectTraceAct) {
            ObjectDef lastCorrectNode = lastCorrectTraceAct.getRelationshipLink("hasCFGNode").getObjects().getFirst();
            ObjectDef endOfProgram = findEndOfProgram(model);

            var fromCorrect_toEnd = model.getObjects().stream()
                    .filter(obj -> obj.getClassName().equals("PathInfo"))
                    .filter(path ->
                            path.getRelationshipLink("from_").getObjects().getFirst().equals(lastCorrectNode) &&
                                    path.getRelationshipLink("to_").getObjects().getFirst().equals(endOfProgram))
                    .max(Comparator.comparingInt(pi -> (Integer) pi.getPropertyValue("opaque_actions", Map.of())));

            if (fromCorrect_toEnd.isPresent()) {
                return (Integer) fromCorrect_toEnd.get().getPropertyValue("opaque_actions", Map.of());
            }
            return 5; // fallback
        }

        int calculateInteractionsLeft(DomainModel model, ObjectDef lastCorrectTraceAct) {
            // По умолчанию: простой подсчёт по цепочке directlyBeforeOf
            return calculateInteractionsLeftByTrace(lastCorrectTraceAct);

            // Альтернатива (закомментировать):
            // return calculateInteractionsLeftByPath(model, lastCorrectTraceAct);
        }

        @Override
        public void updateJudgeInterpretationResult(InterpretSentenceResult interpretationResult, DecisionTreeReasonerBackend.Output backendOutput) {
            ObjectDef L0TraceAct = backendOutput.situation().getDecisionTreeVariables().get("L0")
                    .findIn(backendOutput.situation().getDomainModel());

            // Если ответ правильный, берём следующий за L0 элемент трассы (идентичен A по позиции, но имеет связи трассы)
            ObjectDef traceActForCount = interpretationResult.isAnswerCorrect
                    ? L0TraceAct.getRelationshipLink("directlyBeforeOf").getObjects().getFirst()
                    : L0TraceAct;

            interpretationResult.CountCorrectOptions = 1;
            int finishButtonEnabled = 1; // Note: set 0 if using explicit "Finish the problem" button.

            interpretationResult.IterationsLeft = calculateInteractionsLeft(
                    backendOutput.situation().getDomainModel(),
                    traceActForCount) - finishButtonEnabled;

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
            ObjectDef referenceA = currentTraceAct.getRelationshipLink("directlyBeforeOf").getObjects().getFirst();

            ObjectDef A = makeA(questionModel, responses.getLast().getLeftAnswerObject().getDomainInfo(), referenceA);
            ObjectDef L0 = currentTraceAct;

            updateModelState(domain, questionModel, L0, A);

            return new DecisionTreeReasonerBackend.Input(questionModel, model.getDecisionTree());
        }

        void updateModelState(ControlFlowDTDomain domain, DomainModel questionModel, ObjectDef L0, ObjectDef A) {
            ObjectDef STATE = questionModel.getVariables().get("STATE").getValueObject();
            ObjectDef traceActPtr = findStartOfProgram(questionModel);
            ObjectDef prev;
            while (true) {
                if (traceActPtr.getName().equals(L0.getName())) {
                    break;
                }
                prev = traceActPtr;
                traceActPtr = traceActPtr.getRelationshipLink("directlyBeforeOf")
                        .getObjects().getFirst();
                @Nullable ObjectDef path = domain.findPathInfo(questionModel,
                        prev.getRelationshipLink("hasCFGNode").getObjects().getFirst(),
                        traceActPtr.getRelationshipLink("hasCFGNode").getObjects().getFirst()
                ).orElse(null);
                if (path == null || path.getRelationshipLinks().stream().noneMatch(r -> r.getRelationshipName().equals("hasEffects"))) continue;
                var intrptStart = path.getRelationshipLink("hasEffects")
                        .getObjects().getFirst()
                        .getPropertyValue("interruption_start", Map.of());
                var interptStop = path.getRelationshipLink("hasEffects")
                        .getObjects().getFirst()
                        .getPropertyValue("interruption_stop", Map.of());

                EnumValueRef noInterrupt = questionModel.getEnums().get("InterruptionType").getValues().get("no_interruption").getReference();
                // Эффекты из узлов CFG.
                // (Пока только для конечного узла.!)
                if (interptStop.equals(noInterrupt)) {
                    try {
                    // Нет остановки прерывания на пути.
                    // Попытаемся получить эффекты из узла.
                    var targetNode = traceActPtr.getRelationshipLink("hasCFGNode").getObjects().getFirst();
                    if (targetNode.getRelationshipLinks().stream().anyMatch(r -> r.getRelationshipName().equals("hasEffects"))) {

                        var targetNodeEffects_L = targetNode.getRelationshipLink("hasEffects").getObjects();
                        if (!targetNodeEffects_L.isEmpty()) {
                            var targetNodeEffects = targetNodeEffects_L.getFirst();
                            interptStop = targetNodeEffects.getPropertyValue("interruption_stop", Map.of());
                        }
                    }
                    } catch (its.model.definition.DomainNonConformityException ignored) {}
                }

                PropertyValueStatement<ObjectDef> oldInterpt = STATE.getDefinedPropertyValues().get("interruption_state", Map.of());
                Object oldInterpt_2 = null; // = oldInterpt.getValue();

                if (!intrptStart.equals(oldInterpt.getValue())) {
                    STATE.getDefinedPropertyValues().addOrReplace(
                            new PropertyValueStatement<>(STATE, "interruption_state", ParamsValues.getEMPTY(), intrptStart)
                    );
                    // Запомним изменившееся состояние.
                    oldInterpt_2 = intrptStart;
                }
                if (!interptStop.equals(noInterrupt) && (interptStop.equals(oldInterpt.getValue()) || interptStop.equals(oldInterpt_2))) {
                    STATE.getDefinedPropertyValues().addOrReplace(
                            new PropertyValueStatement<>(STATE, "interruption_state", ParamsValues.getEMPTY(), noInterrupt)
                    );
                }
            }
            ///
            // System.out.println(STATE.getDefinedPropertyValues().stream().map(Statement::toString).collect(Collectors.joining()));
            ///

            // Выставляем итоговые переменные
            questionModel.getVariables().add(new VariableDef("A", A.getName()));
            questionModel.getVariables().add(new VariableDef("L0", L0.getName()));
        }

        ObjectDef makeTrace(DomainModel questionModel, List<ResponseEntity> responses, boolean includeLast) {
            ObjectDef firstTraceAct = findStartOfProgram(questionModel);

            ObjectDef currentTraceAct;
            int end = includeLast ? responses.size() : responses.size() - 1;
            if (end <= 0) {
                currentTraceAct = firstTraceAct;
            } else {
                currentTraceAct = firstTraceAct.getRelationshipLink("directlyBeforeOf").getObjects().getFirst();
            }
            for (int i = 0; i < end; i++) {
                ResponseEntity response = responses.get(i);
                String domainInfo = response.getLeftAnswerObject().getDomainInfo();
                ObjectDef cfgNode = questionModel.getObjects().stream()
                        .filter(obj -> obj.getClassName().equals("Node"))
                        .filter(obj -> obj.getPropertyValue("id", Map.of()).equals(domainInfo))
                        .findFirst().orElseThrow();
                ObjectDef currentTraceActCFGNode = currentTraceAct.getRelationshipLink("hasCFGNode")
                        .getObjects().getFirst();
                if (currentTraceActCFGNode.equals(cfgNode)) {
                    currentTraceAct.getDefinedPropertyValues().addOrReplace(new PropertyValueStatement<>(
                            currentTraceAct,
                            "is_known_correct",
                            ParamsValues.getEMPTY(), true));
                } else if (includeLast && (i + 1) != end) { // подавляем эту проверку, если мы намеренно встраиваем последний акт в трассу
                    throw new DomainUseException("Invalid correct-trace: act is in already checked/used acts: currentTraceAct = %s with hasCFGNode.id = %s, expected that hasCFGNode.id = %s".formatted(currentTraceAct.getName(), currentTraceActCFGNode.getName(), domainInfo));
                }
                if ((i + 1) != end) {
                    currentTraceAct = currentTraceAct.getRelationshipLink("directlyBeforeOf").getObjects().getFirst();
                }
            }
            return currentTraceAct;
        }

        ObjectDef makeA(DomainModel questionModel, String cfgNodeId, @Nullable ObjectDef referenceTraceAct) {
            ObjectDef result = DomainBuilderUtils.newObject(questionModel, "trace_act_A", "TraceAct");
            ObjectDef cfgNode = questionModel.getObjects().stream()
                    .filter(obj -> obj.getClassName().equals("Node"))
                    .filter(obj -> obj.getPropertyValue("id", Map.of()).equals(cfgNodeId))
                    .findFirst().orElseThrow();
            ObjectDef metadata = cfgNode.getRelationshipLink("hasMetadata").getObjects().getFirst();
            EnumValueRef noValue = questionModel.getEnums().get("OptionalBoolValue").getValues().get("no_value").getReference();
            result.getDefinedPropertyValues().addOrReplace(new PropertyValueStatement<>(result, "is_known_correct", ParamsValues.getEMPTY(), false));
            result.getDefinedPropertyValues().addOrReplace(new PropertyValueStatement<>(result, "condition_value", ParamsValues.getEMPTY(), noValue));
            if (referenceTraceAct != null) {
                String refCfgNodeId = (String) referenceTraceAct.getRelationshipLink("hasCFGNode")
                        .getObjects()
                        .getFirst()
                        .getPropertyValue("id", Map.of());
                if (refCfgNodeId.equals(cfgNodeId)) { // корректное действие выбрано как A
                    result.getDefinedPropertyValues().addOrReplace(new PropertyValueStatement<>(result, "condition_value",
                            ParamsValues.getEMPTY(), referenceTraceAct.getPropertyValue("condition_value", Map.of())
                    ));
                    if (referenceTraceAct.getRelationshipLinks().stream().anyMatch(r -> r.getRelationshipName().equals("hasRuntimeInfo"))
                        && !referenceTraceAct.getRelationshipLink("hasRuntimeInfo").getObjects().isEmpty()) {
                        result.getRelationshipLinks().add(new RelationshipLinkStatement(result, "hasRuntimeInfo",
                                List.of(referenceTraceAct.getRelationshipLink("hasRuntimeInfo").getObjects().getFirst().getName()),
                                ParamsValues.getEMPTY()));
                    }
                }
            }
            result.getRelationshipLinks().add(new RelationshipLinkStatement(result, "hasCFGNode", List.of(cfgNode.getName()), ParamsValues.getEMPTY()));
            result.getRelationshipLinks().add(new RelationshipLinkStatement(result, "hasActionSpec",
                    List.of(metadata.getRelationshipLink("hasAbstractAction").getObjects().getFirst().getName()),
                    ParamsValues.getEMPTY()));
            result.getRelationshipLinks().add(new RelationshipLinkStatement(result, "hasASTNode",
                    List.of(metadata.getRelationshipLink("belongsToASTNode").getObjects().getFirst().getName()),
                    ParamsValues.getEMPTY()));
            result.getMetadata().addAll(cfgNode.getMetadata());
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
            var exerciseOptions = exerciseAttempt.getExercise().getOptions();
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
                questionRequest.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
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
        result.getQuestionData().setExerciseAttempt(exerciseAttemptEntity);
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
            LearningSituation situation = new LearningSituation(model, model.getVariables()
                    .stream().collect(Collectors.toMap(
                            x -> x.getName(),
                            x -> x.getValueObject().getReference()
                    ))
            );
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
        ObjectDef A = treeInterface.makeA(questionModel, cfgId, nextAnswer);
        treeInterface.updateModelState(this, questionModel, L0, A);
        var solveRes = Solver.solve(ControlFlowDTDomain.this.getDomainSolvingModels().getFirst().getDecisionTree(), questionModel);
        Explanation explanation = DecisionTreeReasonerBackend.collectExplanationsFromTrace(
                Explanation.Type.HINT,
                solveRes.trace(), questionModel,
                this, deniedSkills, lang
        );
        AnswerObjectEntity answer = q.getAnswerObjects().stream().filter(ans -> ans.getDomainInfo().equals(cfgId)).findFirst().orElse(null);
        correctAnswer.answers = List.of(new CorrectAnswer.Response(answer, answer));
        correctAnswer.question = q.getQuestionData();
        correctAnswer.lawName = null;
        correctAnswer.skillName = solveRes.skills();
        correctAnswer.explanation = explanation;
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

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Форматирует информацию RuntimeInfo для отображения в трассе.
     * @param runtimeInfo объект RuntimeInfo из модели
     * @return отформатированная строка или пустая строка, если нет данных
     */
    private String formatRuntimeInfo(ObjectDef runtimeInfo, Language lang) {
        if (runtimeInfo == null) return "";
        final int MAX_OUTPUT_LENGTH = 12;
        final boolean DISABLE_PRINT = true;

        StringBuilder sb = new StringBuilder();
        String funcName = (String) runtimeInfo.getPropertyValue("function_name", Map.of());
        String funcArgs = (String) runtimeInfo.getPropertyValue("function_args", Map.of());
        String returnValue = (String) runtimeInfo.getPropertyValue("return_value", Map.of());
        String printOutputs = (String) runtimeInfo.getPropertyValue("print_outputs", Map.of());

        boolean hasFunc = funcName != null && !funcName.isEmpty() && funcArgs != null && !funcArgs.isEmpty();
        boolean hasReturn = returnValue != null && !returnValue.isEmpty();
        boolean hasPrint = printOutputs != null && !printOutputs.isEmpty();

        Predicate<StringBuilder> isEmptySb = (StringBuilder s) -> s.toString().endsWith(" — ") || s.toString().endsWith("# ");

        if (hasPrint || hasReturn) {
            sb.append("# ");
        } else {
            sb.append(" — ");
        }

        // Формат вызова: fact(5)
        if (hasFunc) {
            sb.append(funcName).append("(");
            // Парсим JSON {"n": 5} -> выводим значения: 5
            try {
                JsonNode json = jsonMapper.readTree(funcArgs);
                List<String> values = new ArrayList<>();
                json.fields().forEachRemaining(entry -> {
                    JsonNode val = entry.getValue();
                    String text = val.isTextual() ? val.asText() : val.toString();
                    if (text.length() > MAX_OUTPUT_LENGTH) {
                        values.add("...");
                    } else {
                        values.add(text);
                    }
                });
                sb.append(String.join(", ", values));
            } catch (Exception e) {
                sb.append(funcArgs);
            }
            sb.append(")");
        }

        // Формат возврата: -> 120
        if (hasReturn) {
            if (!isEmptySb.test(sb)) sb.append(", ");
            sb.append(getMessage("trace.template.return_value", lang)).append(": ").append(returnValue);
        }

        // Формат print: ["fact(5) = 120"] -> print: fact(5) = 120
        if (hasPrint && !DISABLE_PRINT) {
            if (!isEmptySb.test(sb)) sb.append(", ");
            sb.append(getMessage("trace.template.print_value", lang)).append(": ");
            try {
                JsonNode arr = jsonMapper.readTree(printOutputs);
                if (arr.isArray() && arr.valueStream()
                        .map(JsonNode::asText)
                        .collect(Collectors.summingInt(String::length)) <= 2 * MAX_OUTPUT_LENGTH
                ) {
                    for (JsonNode el : arr) {
                        sb.append(el.asText());
                    }
                }
            } catch (Exception e) {
                sb.append(printOutputs);
            }
        }

        return sb.toString();
    }

    protected List<ResponseEntity> responsesForTrace(QuestionEntity q, boolean allowLastIncorrect) {

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

    public String getMessageWithSuffix(String rawName, String suffix, Language lang) {
        var rawNameFilled = rawName.concat(suffix);
        var message = getMessage(rawNameFilled, lang);
        if (message == null || message.isEmpty() || message.equals(rawNameFilled)) {
            message = getMessage(rawName, lang);
        }
        return message;
    }

    @Override
    public List<HyperText> getFullSolutionTrace(Question question) {
        Language lang = Optional.ofNullable(question.getQuestionData().getExerciseAttempt())
                .map(a -> a.getUser().getPreferred_language())
                .orElse(Language.RUSSIAN/*ENGLISH*/);
        List<HyperText> trace = new ArrayList<>();
        var questionModel = prepareQuestionModel(question, this.domainSolvingModel);
        var treeInterface = (DecisionTreeInterface) getBackendInterface();
        var responses = responsesForTrace(question.getQuestionData(), true);
        var lastTraceObj = treeInterface.makeTrace(questionModel, responses, false);
        var referenceA = lastTraceObj.getRelationshipLink("directlyBeforeOf").getObjects().getFirst();
        ObjectDef A = responses.isEmpty() ? null : treeInterface.makeA(questionModel, responses.getLast().getLeftAnswerObject().getDomainInfo(), referenceA);
        List<ObjectDef> traceObjects = new ArrayList<>(questionModel.getObjects()
                .stream()
                .filter(obj -> obj.getClassName().equals("TraceAct")).toList());
        // WARNING! TODO: сейчас полагаемся на порядок сортировки в loqi файле. Это потенциально плохо! Исправить в будущем
        if (A != null) {
            questionModel.getVariables().add(new VariableDef("A", A.getName()));
        }
        var counters = new HashMap<String, Integer>();
        for (var object : traceObjects) {
            if ((Boolean) object.getPropertyValue("is_known_correct", Map.of()) ||
                    (questionModel.getVariables().get("A") != null && questionModel.getVariables().get("A").getValueObject().getName().equals(object.getName()))
            ) {
                var action = object.getRelationshipLinks().stream().filter(x ->
                        x.getRelationship().getName().equals("hasActionSpec")).findFirst().orElseThrow().getObjects().getFirst();
                var construct = action.getRelationshipLinks().stream().filter(x ->
                        x.getRelationship().getName().equals("hasConstruct")).findFirst().orElseThrow().getObjects().getFirst();
                var cfgNode = object.getRelationshipLinks().stream().filter(x ->
                        x.getRelationship().getName().equals("hasCFGNode")).findFirst().orElseThrow().getObjects().getFirst();
                var cfgId = (String) cfgNode.getPropertyValue("id", Map.of());
                int n = counters.merge(cfgId, 1, Integer::sum);
                var constructKind = Arrays.stream(
                        ((String) construct.getPropertyValue("kind", Map.of())).split("\\.")
                ).toList();

                var mainString = getMessage("trace.template", lang);

                var localeTraceName = (String) action.getPropertyValue("_locale_trace_name", Map.of());
                if (localeTraceName.isEmpty()) {
                    localeTraceName = (String) construct.getPropertyValue("_locale_trace_name", Map.of());
                }
                var localePronoun = (String) action.getPropertyValue("_locale_pronoun", Map.of());
                if (localePronoun.isEmpty()) {
                    localePronoun = (String) construct.getPropertyValue("_locale_pronoun", Map.of());
                }

                String actionState;
                String condition = "";
                String nodeKind = ((EnumValueRef) cfgNode.getPropertyValue("kind", Map.of())).getValueName();
                String rawNameSuffix = "";
                if (localeTraceName.equals("program")) {
                    rawNameSuffix = "_program";
                } else if (localePronoun.equals("he")) {
                    rawNameSuffix = ".he";
                } else if (localePronoun.equals("she")) {
                    rawNameSuffix = ".she";
                }
                if (localeTraceName.equals("condition")) {
                    String conditionEnumValue = ((EnumValueRef) object.getPropertyValue("condition_value", Map.of())).getValueName();
                    if (conditionEnumValue.equals("true") || conditionEnumValue.equals("false")) {
                        condition = " - ".concat(htmlStyleFormat(getMessage("trace.condition.%s".formatted(conditionEnumValue), lang), "atom"));
                    }
                    actionState = getMessageWithSuffix("trace.evaluated", rawNameSuffix, lang);
                } else if (nodeKind.equals("atom")) {
                    actionState = getMessageWithSuffix("trace.executed", rawNameSuffix, lang);
                } else {
                    boolean isEnd = nodeKind.equals("END");
                    if (isEnd) {
                        actionState = getMessageWithSuffix("trace.ended", rawNameSuffix, lang);
                    } else {
                        actionState = getMessageWithSuffix("trace.began", rawNameSuffix, lang);
                    }
                }

                // Извлечение RuntimeInfo
                String runtimeInfoStr = "";
                var runtimeInfoLinks = object.getRelationshipLinks().stream()
                        .filter(x -> x.getRelationship().getName().equals("hasRuntimeInfo"))
                        .findFirst();
                if (runtimeInfoLinks.isPresent() && !runtimeInfoLinks.get().getObjects().isEmpty()) {
                    ObjectDef runtimeInfo = runtimeInfoLinks.get().getObjects().getFirst();
                    runtimeInfoStr = formatRuntimeInfo(runtimeInfo, lang);
                    if (!runtimeInfoStr.isEmpty()) {
                        runtimeInfoStr = htmlStyleFormat(runtimeInfoStr,
                                runtimeInfoStr.startsWith("#") ? "runtime-info-comment" : "runtime-info");
                    }
                }

                String nthTime = htmlStyleFormat(formatNthTime(n, lang), "number") + " " + getMessage("trace.template.time_text", lang);
                String definition = (String) object.getMetadata().get(lang.toLocaleString().toUpperCase(), "localizedName");
                var substitutions = Map.of(
                        "structure", htmlStyleFormat(definition, "action"),
                        "action_state", htmlStyleFormat(actionState, "keyword"),
                        "nth_time", nthTime,
                        "runtime_extra", runtimeInfoStr,
                        "condition", condition
                );
                var traceElementText = replaceInString(mainString, substitutions);
                if (!(Boolean) object.getPropertyValue("is_known_correct", Map.of())
                        && !responses.getLast().getInteraction().getViolations().isEmpty()
                ) {
                    traceElementText = htmlStyleFormat(traceElementText, "warning");
                }
                trace.add(new HyperText(traceElementText));
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
