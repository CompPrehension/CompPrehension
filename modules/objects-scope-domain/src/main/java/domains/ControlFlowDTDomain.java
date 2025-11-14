package domains;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.definition.loqi.DomainLoqiBuilder;
import its.reasoner.LearningSituation;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.Service.LocalizationService;
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

    Map<String, Skill> skills;

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

    private static class DecisionTreeInterface implements DecisionTreeReasonerBackend.Interface {

        @Override
        public InterpretSentenceResult interpretJudgeNotPerformed(Question judgedQuestion, LearningSituation preparedSituation) {
            return null;
        }

        @Override
        public void updateJudgeInterpretationResult(InterpretSentenceResult interpretationResult, DecisionTreeReasonerBackend.Output backendOutput) {
            interpretationResult.CountCorrectOptions = 1;
            interpretationResult.IterationsLeft = 10;

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
            return new DecisionTreeReasonerBackend.Input(domain.prepareQuestionModel(question, model), model.getDecisionTree());
        }

        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForSolve(Question question, List<Tag> tags) {
            return null;
        }
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

    public void fillSkills() {
        skills = new HashMap<>();
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

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        return null;
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
