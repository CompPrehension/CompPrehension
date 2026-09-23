package org.vstu.compprehension.businesslogic.domains;

import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.data.question.BackendFactData;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.vstu.compprehension.businesslogic.domains.helpers.DomainSolvingModelLoader;
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
import lombok.extern.log4j.Log4j2;
import lombok.val;
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
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.services.LocalizationService;
import org.vstu.compprehension.businesslogic.*;
import org.vstu.compprehension.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.businesslogic.backend.Fact;
import org.vstu.compprehension.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.businesslogic.HyperText;

import java.io.StringReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.vstu.compprehension.data.question.GeneratedQuestionData;
import org.vstu.compprehension.data.question.QuestionContentData;

@Log4j2
public class ControlFlowDTDomain extends DecisionTreeReasoningDomain {
    private final DecisionTreeInterface backendInterface = new DecisionTreeInterface();
    protected final LocalizationService localizationService;
    protected final QuestionBank qMetaStorage;

    static final String RESOURCES_LOCATION = "domains/";
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String MESSAGE_PREFIX = "ctrlflow_text.";
    public static final String DOMAIN_ID = "ctrl_flow_dt25";
    static final String LOCALIZED_NAME = "localizedName";
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

    public DomainModel prepareQuestionModel(QuestionContentData q, DomainSolvingModel domainSolvingModel) {
        var loqiText = q.getStatementFacts().getFirst().getObject();
        DomainModel commonModel = domainSolvingModel.getDomainModel();
        DomainModel resultModel = commonModel.copy();
        DomainModel situationModel;
        try (StringReader reader = new StringReader(loqiText)) {
            situationModel = DomainLoqiBuilder.buildDomain(reader);
        }
        resultModel.addMerge(situationModel);
        fillMissingLocalizedNames(resultModel);
        return resultModel;
    }

    private static void fillMissingLocalizedNames(DomainModel model) {
        for (ObjectDef object : model.getObjects()) {
            Map<String, String> localizations = object.getMetadata().getStringLocalizations(LOCALIZED_NAME);
            if (localizations.isEmpty()) {
                continue;
            }
            String fallback = Arrays.stream(Language.values())
                    .map(Language::toLocaleString)
                    .filter(localizations::containsKey)
                    .findFirst()
                    .map(localizations::get)
                    .orElseGet(() -> localizations.values().iterator().next());
            for (Language language : Language.values()) {
                String locale = language.toLocaleString();
                if (!localizations.containsKey(locale)) {
                    object.getMetadata().add(locale, LOCALIZED_NAME, fallback);
                }
            }
        }
    }

    private static Map<String, Skill> buildSkills() {
        var b = new SkillsBuilder();
        var visible = EnumSet.of(DomainItemFlag.VISIBLE_TO_TEACHER);

        b.add("current_execution_point_understood", 0x1L, visible);
        b.add("current_code_block_identified", 0x2L, visible);
        b.add("action_execution_determined", 0x4L, visible);
        b.add("unevaluated_conditions_between_points_present", 0x8L, visible);
        b.add("transition_applicable_regardless_interruption", 0x10L, visible);
        b.add("interruption_type_matched", 0x20L, visible);
        b.add("applicable_transition_with_interruption_found", 0x40L, visible);
        b.add("action_is_condition_recognized", 0x80L, visible);
        b.add("required_condition_value_determined", 0x100L, visible);
        b.add("applicable_transition_with_condition_found", 0x200L, visible);
        b.add("action_is_function_call_recognized", 0x400L, visible);
        b.add("function_body_completion_determined", 0x800L, visible);
        b.add("nearest_function_call_in_trace_found", 0x1000L, visible);
        b.add("transition_matches_interruption_mode", 0x2000L, visible);
        b.add("compound_structure_boundaries_identified", 0x4000L, visible);
        b.add("early_exit_from_compound_detected", 0x8000L, visible);
        b.add("multiple_compound_exit_order_understood", 0x10000L, visible);
        b.add("applicable_compound_exit_determined", 0x20000L, visible);
        b.add("interruption_termination_recognized", 0x40000L, visible);
        b.add("interruption_can_be_terminated_determined", 0x80000L, visible);

        // для целей отладки, еще не удалены из дерева
        b.add("unknown_incorrect", 0x100000L);  // неизвестная ошибка
        b.add("unknown_correct", 0x200000L);   // не знает почему правильно
        b.add("patch_stop_as_true", 0x400000L);
        b.add("patch_auto_exit_interruption", 0x800000L);
        b.add("patch_auto_exit_interruption_2", 0x1000000L);
        b.add("patch_auto_exit_interruption_3", 0x2000000L);
        b.add("selected_transition_without_any_constraint", 0x4000000L);

        return b.build();
    }

    private static Map<String, Concept> buildConcepts() {
        var b = new ConceptsBuilder();

        var flags = EnumSet.of(DomainItemFlag.VISIBLE_TO_TEACHER, DomainItemFlag.TARGET_ENABLED);
        var invisible = EnumSet.of(DomainItemFlag.TARGET_ENABLED);

        Concept exprs = b.add("expressions");
        b.add("pointers", 0x1L, List.of(exprs), flags);
        b.add("bitwise", 0x2L, List.of(exprs), flags);
        b.add("member_access", 0x4L, List.of(exprs), flags);
        b.add("arrays", 0x8L, List.of(exprs), flags);
        b.add("type_casts", 0x10L, List.of(exprs), flags);
        b.add("io", 0x20L, List.of(exprs), flags);
        b.add("logical", 0x40L, List.of(exprs), flags);
        b.add("arithmetic", 0x80L, List.of(exprs), flags);
        b.add("object_new", 0x400000000L, List.of(exprs), flags);
        b.add("strings", 0x800000000L, List.of(exprs), flags);
        b.add("map_collections", 0x1000000000L, List.of(exprs), flags);
        b.add("ternary_conditions", 0x100L, List.of(exprs), flags);
        Concept functionCall = b.add("function_call", List.of(exprs));
        b.add("lib_function_call", 0x200L, List.of(functionCall), flags);     // функции в задаче нет
        b.add("program_function_call", 0x400L, List.of(functionCall), flags); // функция определена в задаче

        Concept plain = b.add("plain_statements");
        b.add("var_declaration", 0x800L, List.of(plain), flags);
        b.add("assignment", 0x1000L, List.of(plain), flags);
        b.add("break", 0x2000L, List.of(plain), flags);
        b.add("continue", 0x4000L, List.of(plain), flags);
        b.add("return", 0x8000L, List.of(plain), flags);
        b.add("delete", 0x10000L, List.of(plain), flags);

        Concept structures = b.add("structures");
        Concept function = b.add("function", 0x20000L, List.of(structures), flags);
        b.add("recursion", 0x2000000000L, List.of(function), flags);
        b.add("structure", 0x40000L, List.of(structures), flags);
        Concept oop = b.add("objects", List.of(structures), flags);
        b.add("class", 0x80000L, List.of(oop), invisible);
        b.add("field", 0x100000L, List.of(oop), invisible);
        b.add("method", 0x200000L, List.of(oop), invisible);

        Concept loops = b.add("loops");
        b.add("loop_iteration", 0x4000000000L, List.of(loops), flags);
        Concept forLoop = b.add("for_loop", List.of(loops));
        Concept forGeneralLoop = b.add("general_for_loop", 0x400000L, List.of(forLoop), flags);
        b.add("range_for_loop", 0x800000L, List.of(forGeneralLoop), flags);
        b.add("for_each_loop", 0x1000000L, List.of(forLoop), flags);
        b.add("while_loop", 0x2000000L, List.of(loops), flags);
        b.add("do_while_loop", 0x4000000L, List.of(loops), flags);
        b.add("infinite_loop", 0x8000000L, List.of(loops), flags);

        Concept branches = b.add("branches");
        b.add("if", 0x10000000L, List.of(branches), flags);
        b.add("else", 0x20000000L, List.of(branches), flags);
        b.add("elseif", 0x40000000L, List.of(branches), flags);
        Concept switches = b.add("switch", 0x80000000L, List.of(branches), flags);
        b.add("fallthrough_case", 0x100000000L, List.of(switches), flags);
        b.add("default_case", 0x200000000L, List.of(switches), flags);

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
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForJudge(QuestionData question, List<? extends AnswerData> responses, List<Tag> tags) {
            if (question.getContent().getMetadata().getVersion() != 2) {
                throw new UnsupportedOperationException("Unsupported version of CtrlFlow question");
            }
            var domain = ControlFlowDTDomain.this;
            var model = domain.getDomainSolvingModels().getFirst();

            DomainModel questionModel = domain.prepareQuestionModel(question.getContent(), model);

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

        ObjectDef makeTrace(DomainModel questionModel, List<? extends AnswerData> responses, boolean includeLast) {
            ObjectDef firstTraceAct = findStartOfProgram(questionModel);

            ObjectDef currentTraceAct;
            int end = includeLast ? responses.size() : responses.size() - 1;
            if (end <= 0) {
                currentTraceAct = firstTraceAct;
            } else {
                currentTraceAct = firstTraceAct.getRelationshipLink("directlyBeforeOf").getObjects().getFirst();
            }
            for (int i = 0; i < end; i++) {
                AnswerData response = responses.get(i);
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
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForSolve(QuestionContentData question, List<Tag> tags) {
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
        var questionDomainType = question.getContent().getQuestionDomainType();
        if (questionDomainType.equals(EXECUTION_ORDER_QUESTION_TYPE)) {
            List<Fact> result = new ArrayList<>();
            for (AnswerData response : responses) {
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
    public CorrectAnswer getAnyNextCorrectAnswer(QuestionData q, Language language) {
        List<ResponseData> responses = q.latestCorrectResponses();
        var model = getDomainSolvingModels().getFirst();
        ControlFlowDTDomain.DecisionTreeInterface treeInterface = (ControlFlowDTDomain.DecisionTreeInterface) getBackendInterface();
        DomainModel questionModel = prepareQuestionModel(q.getContent(), model);
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
                this, language
        );
        AnswerObjectData answer = q.getContent().getAnswerObjects().stream().filter(ans -> ans.getDomainInfo().equals(cfgId)).findFirst().orElse(null);
        correctAnswer.answers = List.of(new CorrectAnswer.Response(answer, answer));
        correctAnswer.question = q;
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
                json.properties().forEach(entry -> {
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

    protected List<ResponseData> responsesForTrace(QuestionData q, boolean allowLastIncorrect) {

        List<ResponseData> responses = new ArrayList<>();
        List<QuestionInteractionData> interactions = q.getInteractions();

        if (interactions == null || interactions.isEmpty()) {
            return responses; // empty so far
            // early exit: no further checks for emptiness
        }

        responses = Optional.of(interactions).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0) // select only interactions without mistakes
                .reduce((first, second) -> second)
                .map(QuestionInteractionData::getResponses)
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

    @NotNull
    @Override
    public List<HyperText> getFullSolutionTrace(@NotNull QuestionData question, @NotNull Language language) {
        List<HyperText> trace = new ArrayList<>();
        var questionModel = prepareQuestionModel(question.getContent(), this.domainSolvingModel);
        var treeInterface = (DecisionTreeInterface) getBackendInterface();
        var responses = responsesForTrace(question, true);
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

                var mainString = getMessage("trace.template", language);

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
                if (localeTraceName.startsWith("condition")) {
                    String conditionEnumValue = ((EnumValueRef) object.getPropertyValue("condition_value", Map.of())).getValueName();
                    if (conditionEnumValue.equals("true") || conditionEnumValue.equals("false")) {
                        condition = " - ".concat(htmlStyleFormat(getMessage("trace.condition.%s".formatted(conditionEnumValue), language), "atom"));
                    }
                    actionState = getMessageWithSuffix("trace.evaluated", rawNameSuffix, language);
                } else if (nodeKind.equals("atom")) {
                    actionState = getMessageWithSuffix("trace.executed", rawNameSuffix, language);
                } else {
                    boolean isEnd = nodeKind.equals("END");
                    if (isEnd) {
                        actionState = getMessageWithSuffix("trace.ended", rawNameSuffix, language);
                    } else {
                        actionState = getMessageWithSuffix("trace.began", rawNameSuffix, language);
                    }
                }

                // Извлечение RuntimeInfo
                String runtimeInfoStr = "";
                var runtimeInfoLinks = object.getRelationshipLinks().stream()
                        .filter(x -> x.getRelationship().getName().equals("hasRuntimeInfo"))
                        .findFirst();
                if (runtimeInfoLinks.isPresent() && !runtimeInfoLinks.get().getObjects().isEmpty()) {
                    ObjectDef runtimeInfo = runtimeInfoLinks.get().getObjects().getFirst();
                    runtimeInfoStr = formatRuntimeInfo(runtimeInfo, language);
                    if (!runtimeInfoStr.isEmpty()) {
                        runtimeInfoStr = htmlStyleFormat(runtimeInfoStr,
                                runtimeInfoStr.startsWith("#") ? "runtime-info-comment" : "runtime-info");
                    }
                }

                String nthTime = htmlStyleFormat(formatNthTime(n, language), "number") + " " + getMessage("trace.template.time_text", language);
                String definition = object.getMetadata().getString(language.toLocaleString(), LOCALIZED_NAME);
                var substitutions = Map.of(
                        "structure", htmlStyleFormat(definition, "action"),
                        "action_state", htmlStyleFormat(actionState, "keyword"),
                        "nth_time", nthTime,
                        "runtime_extra", runtimeInfoStr,
                        "condition", condition
                );
                var traceElementText = replaceInString(mainString, substitutions);
                if (!(Boolean) object.getPropertyValue("is_known_correct", Map.of())
                        && responses.getLast().isInteractionHasViolations()
                ) {
                    traceElementText = htmlStyleFormat(traceElementText, "warning");
                }
                trace.add(new HyperText(traceElementText));
            }
        }
        return trace;
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
