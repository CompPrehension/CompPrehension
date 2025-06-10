package domains;

import helpers.GenerateErrorTextForScopeObjects;
import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.definition.ObjectDef;
import its.model.definition.ObjectRef;
import its.model.definition.rdf.DomainRDFFiller;
import its.model.nodes.BranchResult;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import its.reasoner.nodes.DecisionTreeTrace;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.domains.DecisionTreeReasoningDomain;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static its.model.definition.build.DomainBuilderUtils.newVariable;
import static its.model.definition.build.DomainBuilderUtils.setBoolProperty;
import static org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph.factsListDeepCopy;

@Log4j2
public class DataFlowDTDomain extends DecisionTreeReasoningDomain {

    static final String DATA_FLOW = "DataFlow";

    static final String RESOURCES_LOCATION = "domains/";

    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "data-flow";

    public static final String VOCAB_SCHEMA_PATH = RESOURCES_LOCATION + "data-flow-domain-schema.rdf";

    protected final LocalizationService localizationService;

    protected final QuestionBank qMetaStorage;

    private Model schemaModel = null;

    static final String MESSAGE_PREFIX = "dataflow_";

    private static final HashMap<String, Tag> tags = new HashMap<>() {{
        put("C++", new Tag("C++", 2L));  	// (2 ^ 1)
    }};

    public static final String QUESTIONS_CONFIG_PATH = RESOURCES_LOCATION + "data-flow-domain-questions.json";
    static List<Question> QUESTIONS;

    private static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "data-flow-domain-model/";

    private final DomainSolvingModel domainSolvingModel = new DomainSolvingModel(
            this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION),
            DomainSolvingModel.BuildMethod.DICT_RDF
    ).validate();

    public DataFlowDTDomain(
            DomainEntity domainEntity,
            LocalizationService localizationService,
            RandomProvider randomProvider,
            QuestionBank qMetaStorage
    ) {
        super(domainEntity, randomProvider, null);

        this.localizationService = localizationService;
        this.qMetaStorage = qMetaStorage;
        this.setBackendInterface(new DecisionTreeInterface());

        fillConcepts();
        fillSkills();

        readLaws();
    }

    private void readLaws() {
        positiveLaws = new HashMap<>();
        negativeLaws = new HashMap<>();
    }

    private void fillConcepts() {
        concepts = new HashMap<>();

        int flags = Concept.FLAG_VISIBLE_TO_TEACHER; //для всех стейджей
        int flagsAll = Concept.FLAG_VISIBLE_TO_TEACHER | Concept.FLAG_TARGET_ENABLED; //для конкретных задач в одном стейдже
        addConcepts(List.of(
                new Concept("pointer", List.of(), flagsAll),
                new Concept("index_access", List.of(), flagsAll),
                new Concept("assignment", List.of(), flagsAll),
                new Concept("assignment_with_modification", List.of(), flagsAll),
                new Concept("prefix_increment_decrement", List.of(), flagsAll),
                new Concept("postfix_increment_decrement", List.of(), flagsAll),
                new Concept("ternary_operator", List.of(), flagsAll),
                new Concept("arithmetic_operator", List.of(), flagsAll),
                new Concept("logic_operator", List.of(), flagsAll),
                new Concept("comparison_operator", List.of(), flagsAll),
                new Concept("function_with_input_data", List.of(), flagsAll),
                new Concept("function_with_output_data", List.of(), flagsAll),
                new Concept("function_with_mutable_data", List.of(), flagsAll)
        ));

        fillConceptTree();

        // assign mask bits to Concepts
        val name2bit = _getConceptsName2bit();
        for (Concept t : concepts.values()) {
            val name = t.getName();
            if (name2bit.containsKey(name)) {
                t.setBitmask(name2bit.get(name));
            }
        }
    }

    public void fillSkills() {
        skills = new HashMap<>();

        addSkill("input_variable_is_at_operator", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("output_variable_is_at_operator", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("mutable_variable_is_at_operator", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("input_variable_is_in_function_call", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("output_variable_is_in_function_call", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("mutable_variable_is_in_function_call", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("mutable_variable_appears_multiple_times", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("input_output_variable_appears_multiple_times", Skill.FLAG_VISIBLE_TO_TEACHER);


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
    public String getDisplayName(Language language) {
        return localizationService.getMessage("dataflow_text.display_name", language);
    }

    @Nullable
    @Override
    public String getDescription(Language language) {
        return localizationService.getMessage("dataflow_text.description", language);
    }

    @Override
    public Collection<Fact> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
        System.out.println("responseToFacts");
        if (questionDomainType.equals(DATA_FLOW)) {
            List<Fact> result = new ArrayList<>();
            for (ResponseEntity response : responses) {
                result.add(new Fact(
                        "owl:NamedIndividual",
                        response.getLeftAnswerObject().getDomainInfo(),
                        "var...",
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
        JenaFactList fl = JenaFactList.fromBackendFacts(q.getQuestionData().getStatementFacts());
        fl.addFromModel(getSchemaForSolving());
        return fl;
    }

    public Model getSchemaForSolving() {
        if (schemaModel == null) {
            // Read & cache the model.
            schemaModel = ModelFactory.createDefaultModel();
            schemaModel.read(VOCAB_SCHEMA_PATH);
        }
        return schemaModel;
    }

    @NotNull
    @Override
    public Question makeQuestion(@NotNull QuestionRequest questionRequest,
                                 @Nullable ExerciseAttemptEntity exerciseAttempt,
                                 @NotNull Language userLanguage) {
        HashSet<String> conceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getTargetConcepts()) {
            conceptNames.add(concept.getName());
        }

        List<QuestionMetadataEntity> foundQuestions = null;
        try {
            int generatorThreshold = (int)(exerciseAttempt.getExercise().getOptions().getMaxExpectedConcurrentStudents() * 1.5);
            foundQuestions = qMetaStorage.searchQuestions(questionRequest, 1, generatorThreshold);

            // search again if nothing found with "TO_COMPLEX"
            SearchDirections lawsSearchDir = questionRequest.getLawsSearchDirection();
            if (foundQuestions.isEmpty() && lawsSearchDir == SearchDirections.TO_COMPLEX) {
                questionRequest.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
                foundQuestions = qMetaStorage.searchQuestions(questionRequest, 1, generatorThreshold);
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
        log.info("Expression domain has prepared the question: {}", res.getName());
        return makeQuestion(res, exerciseAttempt, questionRequest.getTargetTags(), userLanguage);
    }

    @NotNull
    @Override
    public Question makeQuestion(@NotNull QuestionMetadataEntity metadata,
                                 @Nullable ExerciseAttemptEntity exerciseAttemptEntity,
                                 @NotNull List<Tag> tags,
                                 @NotNull Language userLang) {
        var questionData = metadata.getQuestionData();
        return makeQuestion(questionData.getData().toQuestion(this, metadata), exerciseAttemptEntity, tags, userLang);
    }

    protected Question makeQuestion(Question q, ExerciseAttemptEntity exerciseAttemptEntity, List<Tag> tags, Language userLanguage) {
        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showTrace(true)
                .multipleSelectionEnabled(false)
                .showSupplementaryQuestions(false)
                .requireAllAnswers(true)
                .orderNumberOptions(new OrderQuestionOptionsEntity.OrderNumberOptions("", OrderQuestionOptionsEntity.OrderNumberPosition.NONE, null))
                .build();

        QuestionEntity entity = new QuestionEntity();
        entity.setAnswerObjects(q.getAnswerObjects());
        entity.setExerciseAttempt(exerciseAttemptEntity);
        entity.setDomainEntity(getDomainEntity());
        entity.setQuestionDomainType(q.getQuestionDomainType());
        entity.setQuestionName(q.getQuestionName());
        entity.setMetadata(q.getMetadata());
        entity.setTags(tags.stream().map(Tag::getName).collect(Collectors.toList()));

        // DON'T: add schema facts
        List<BackendFactEntity> facts = new ArrayList<>(/*getSchemaFacts(true)*/);
        // statement facts are already prepared in the Question's JSON
        facts.addAll(factsListDeepCopy(q.getStatementFacts()));
        entity.setStatementFacts(facts);
        entity.setQuestionType(q.getQuestionType());
        String text = q.getQuestionText().getText();

        if (Objects.requireNonNull(q.getQuestionType()) == QuestionType.ORDER) {
            entity.setQuestionText(getLocalizedQuestionText(text, userLanguage) + this.ExpressionToHtml(entity.getAnswerObjects(), userLanguage));
            entity.setOptions(orderQuestionOptions);
            return new Question(entity, this);
        }
        throw new UnsupportedOperationException("Unknown type in DataFlowDTDomain::makeQuestion: " + q.getQuestionType());
    }

    private static String getLocalizedQuestionText(String input, Language language) {
        String search = language.toLocaleString() + "{";
        int start = input.indexOf(search);
        if (start == -1) return "";

        start += search.length();
        int braceCount = 1;
        int i = start;

        while (i < input.length() && braceCount > 0) {
            char c = input.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            i++;
        }

        if (braceCount == 0) {
            return input.substring(start, i - 1);
        } else {
            return "";
        }
    }

    public String ExpressionToHtml(List<AnswerObjectEntity> answers, Language userLanguage) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p class='comp-ph-expr'>");
        for (AnswerObjectEntity answer : answers) {
            if(answer.getDomainInfo().isEmpty()) {
                sb.append("<span ")
                        .append("class='comp-ph-expr-const'")
                        .append(">")
                        .append(getMessage("text.variable", userLanguage))
                        .append(" ")
                        .append(answer.getHyperText())
                        .append("</span><br/>");
            } else {
                sb.append("<span ")
                        .append("id='answer_")
                        .append(answer.getAnswerId())
                        .append("' class='comp-ph-expr-op-btn'")
                        .append(">")
                        .append(answer.getHyperText())
                        .append("</span><br/>");
            }
        }
        sb.append("</p>");
        return QuestionTextToHtml(sb.toString(), userLanguage);
    }

    public String QuestionTextToHtml(String text, Language language) {
        StringBuilder sb = new StringBuilder(text
                .replaceAll("\\*", "&#8727")
                .replaceAll("\\n", "<br>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                .replaceAll("text.input", getMessage("text.input", language))
                .replaceAll("text.output", getMessage("text.output", language))
                .replaceAll("text.mutable", getMessage("text.mutable", language))
        );
        sb.insert(0, "<div class='comp-ph-question'>"); sb.append("</div>");
        return sb.toString();
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

        DomainModel situationModel = factsToDomainModel(q.getQuestionData().getStatementFacts());
        for (ResponseEntity response : responses) {
            String[] objects = response.getLeftAnswerObject().getDomainInfo().split(":");
            LearningSituation learningSituation = new LearningSituation(
                    situationModel.copy(),
                    new HashMap<>(Map.of(
                            "answer", new ObjectRef(objects[1]),
                            "var", new ObjectRef(objects[0])
                    ))
            );
            DecisionTreeTrace decisionTreeTrace = DecisionTreeReasoner.solve(
                    domainSolvingModel.getDecisionTree(),
                    learningSituation
            );
            if(decisionTreeTrace.getBranchResult().equals(BranchResult.CORRECT)) {
                val var = situationModel.getDomainModel().getObjects().get(objects[0]);
                setBoolProperty(var, "isEvaluated", true);
            }
        }
        List<ObjectDef> objects = situationModel.getDomainModel().getObjects()
                .stream().filter(objectDef ->
                        objectDef.isInstanceOf("Variable") && !((Boolean) objectDef.getPropertyValue("isEvaluated", Map.of()))
                ).toList();
        // Проверить в ризонере все возможные варианты интеракций и понять, какая из них правильная и выдать подсказку
        String[] listAnswers = {"answerInput", "answerOutput", "answerMutable"};
        for(ObjectDef object : objects) {
            for(String answerVar : listAnswers) {
                newVariable(situationModel, "var", object.getName());
                newVariable(situationModel, "answer", answerVar);

                LearningSituation learningSituation = new LearningSituation(
                        situationModel,
                        LearningSituation.collectDecisionTreeVariables(situationModel)
                );

                DecisionTreeTrace result = DecisionTreeReasoner.solve(
                        domainSolvingModel.getDecisionTree(),
                        learningSituation
                );
                Explanation hintExplanation = GenerateErrorTextForScopeObjects.generateHintExplanationDataFlow(result, learningSituation.getDomainModel(), lang);

                if(!hintExplanation.getChildren().getFirst().getRawMessage().isEmpty()) {
                    AnswerObjectEntity answer = q.getAnswerObjects().stream()
                            .filter(entity -> (object.getName()+":"+answerVar).equals(entity.getDomainInfo()))
                            .findFirst()
                            .orElse(null);
                    CorrectAnswer correctAnswer = new CorrectAnswer();
                    correctAnswer.answers = List.of(new CorrectAnswer.Response(answer, answer));
                    correctAnswer.question = q.getQuestionData();
                    correctAnswer.lawName = null;
                    correctAnswer.skillName = null;

                    Explanation newHintExplanation = replaceEnumInExplanation(hintExplanation.getChildren().getFirst(), lang);
                    correctAnswer.explanation = hintExplanation;
                    correctAnswer.explanation.getChildren().removeFirst();
                    correctAnswer.explanation.getChildren().addFirst(newHintExplanation);
                    return correctAnswer;
                }
                situationModel.getVariables().remove("var");
                situationModel.getVariables().remove("answer");
            }
        }
        AnswerObjectEntity answer = q.getAnswerObjects().stream()
                .findFirst()
                .orElse(null);

        CorrectAnswer correctAnswer = new CorrectAnswer();
        correctAnswer.answers = List.of(new CorrectAnswer.Response(answer, answer));
        correctAnswer.question = q.getQuestionData();
        correctAnswer.lawName = null;
        correctAnswer.skillName = null;
        Explanation explanation = new Explanation(
                Explanation.Type.HINT,
                ""
        );
        explanation.getChildren().add(new Explanation(Explanation.Type.HINT, getMessage("hint_all", lang)));
        correctAnswer.explanation = explanation;
        return correctAnswer;
    }

    private Explanation replaceEnumInExplanation(Explanation explanation, Language language) {
        explanation.setRawMessage(new HyperText(explanation.getRawMessage().getText()
                .replaceAll("text.right", getMessage("text.right", language))
                .replaceAll("text.left", getMessage("text.left", language))
                .replaceAll("text.center", getMessage("text.center", language))
                .replaceAll("text.inputR", getMessage("text.inputR", language))
                .replaceAll("text.outputR", getMessage("text.outputR", language))
                .replaceAll("text.mutableR", getMessage("text.mutableR", language))
                .replaceAll("text.inputI", getMessage("text.inputI", language))
                .replaceAll("text.outputI", getMessage("text.outputI", language))
                .replaceAll("text.mutableI", getMessage("text.mutableI", language))));
        return explanation;
    }

    @Override
    public List<HyperText> getFullSolutionTrace(Question question) {
        return null;
    }

    @NotNull
    @Override
    public Map<String, Tag> getTags() {
        return tags;
    }

    @Override
    public String getMessage(String messageKey, Language preferredLanguage) {
        return localizationService.getMessage(MESSAGE_PREFIX + messageKey, Language.getLocale(preferredLanguage));
    }

    public List<Question> readQuestions(InputStream inputStream) {
        List<Question> res = new ArrayList<>();
        Question[] questions = Arrays.stream(SerializableQuestion.deserializeMany(inputStream))
                .map(q -> q.toQuestion(this))
                .toArray(Question[]::new);
        Collections.addAll(res, questions);
        return res;
    }

    @Override
    protected List<Question> getQuestionTemplates() {
        if (QUESTIONS == null) {
            QUESTIONS = readQuestions(this.getClass().getClassLoader().getResourceAsStream(QUESTIONS_CONFIG_PATH));
        }
        return QUESTIONS;
    }


    //-----Суждение вопросов и подобное ------

    @Override
    public Set<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return new HashSet<>(); //Не нужно для DT
    }

    @Override
    public Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return new HashSet<>(); //Не нужно для DT
    }

    @Override
    public Collection<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        throw new UnsupportedOperationException("no Laws are used for " + this.getClass().getSimpleName());
    }

    @Override
    public Collection<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        return new ArrayList<>(); //Не нужно для DT ?
    }


    //-----------Объяснения---------------
    @Override
    public InterpretSentenceResult interpretSentence(Collection<Fact> violations) {
        return null;
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


    private HashMap<String, Long> _getConceptsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(13);
        name2bit.put("pointer", 0x1L);  		// (1)
        name2bit.put("index_access", 0x2L);  			// (2)
        name2bit.put("assignment", 0x4L);  			// (4)
        name2bit.put("assignment_with_modification", 0x8L);  		// (8)
        name2bit.put("prefix_increment_decrement", 0x10L);  	// (16)
        name2bit.put("postfix_increment_decrement", 0x20L);    // (32)
        name2bit.put("ternary_operator", 0x40L);  // (64)
        name2bit.put("arithmetic_operator", 0x80L);  // (128)
        name2bit.put("logic_operator", 0x100L);  // (256)
        name2bit.put("comparison_operator", 0x200L);    // (512)
        name2bit.put("function_with_input_data", 0x400L);  			// (1024)
        name2bit.put("function_with_output_data", 0x800L);  			// (2048)
        name2bit.put("function_with_mutable_data", 0x1000L);  			// (4096)
        return name2bit;
    }

    private HashMap<String, Long> _getSkillsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(8);
        name2bit.put("input_variable_is_at_operator", 0x1L);  	// (1)
        name2bit.put("output_variable_is_at_operator", 0x2L);  	// (2)
        name2bit.put("mutable_variable_is_at_operator", 0x4L);  	// (4)
        name2bit.put("input_variable_is_in_function_call", 0x8L);  		// (8)
        name2bit.put("output_variable_is_in_function_call", 0x10L);  	// (16)
        name2bit.put("mutable_variable_is_in_function_call", 0x20L);    // (32)
        name2bit.put("mutable_variable_appears_multiple_times", 0x40L);  // (64)
        name2bit.put("input_output_variable_appears_multiple_times", 0x80L);  // (128)
        return name2bit;
    }

    @Override
    public QuestionRequest ensureQuestionRequestValid(QuestionRequest questionRequest) {
        return questionRequest.toBuilder()
                .stepsMin(1)
                .stepsMax(30)
                .build();
    }

    private DomainModel factsToDomainModel(Collection<BackendFactEntity> factEntities) {
        JenaBackend jenaBackend = new JenaBackend();
        JenaFactList jenaFactList = jenaBackend.convertFactEntities(factEntities);
        DomainModel situationModel = domainSolvingModel.getDomainModel().copy();
        DomainRDFFiller.fillDomain(situationModel,
                jenaFactList.getModel(),
                Set.of(DomainRDFFiller.Option.NARY_RELATIONSHIPS_OLD_COMPAT),
                null);
        return situationModel;
    }


    //------ Наводящие вопросы --------
    @Override
    public SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionEntity sourceQuestion, ViolationEntity violation, Language lang) {
        throw new NotImplementedException();
    }

    @Override
    public SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(Question question, SupplementaryStepEntity supplementaryStep, List<ResponseEntity> responses) {
        throw new NotImplementedException();
    }

    @Override
    public boolean needSupplementaryQuestion(ViolationEntity violation) {
        return false;
    }


    private class DecisionTreeInterface extends DecisionTreeReasonerBackend.Interface {
        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForJudge(
                Question question,
                List<ResponseEntity> responses,
                List<Tag> tags
        ) {
            DomainModel situationModel = factsToDomainModel(question.getQuestionData().getStatementFacts());
            ResponseEntity lastResponse = responses.getLast();
            for (ResponseEntity response : responses) {
                if(response != lastResponse) {
                    String[] objects = response.getLeftAnswerObject().getDomainInfo().split(":");
                    LearningSituation learningSituation = new LearningSituation(
                            situationModel.copy(),
                            new HashMap<>(Map.of(
                                    "answer", new ObjectRef(objects[1]),
                                    "var", new ObjectRef(objects[0])
                            ))
                    );
                    DecisionTreeTrace decisionTreeTrace = DecisionTreeReasoner.solve(
                            domainSolvingModel.getDecisionTree(),
                            learningSituation
                    );
                    if(decisionTreeTrace.getBranchResult().equals(BranchResult.CORRECT)) {
                        val var = situationModel.getDomainModel().getObjects().get(objects[0]);
                        setBoolProperty(var, "isEvaluated", true);
                    }
                }
            }
            String[] objects = lastResponse.getLeftAnswerObject().getDomainInfo().split(":");
            newVariable(situationModel, "var", objects[0]);
            newVariable(situationModel, "answer", objects[1]);

            return new DecisionTreeReasonerBackend.Input(
                    situationModel,
                    domainSolvingModel.getDecisionTree()
            );
        }

        @Override
        public InterpretSentenceResult interpretJudgeOutput(Question judgedQuestion, DecisionTreeReasonerBackend.Output backendOutput) {
            if(!backendOutput.isReasoningDone()){
                return interpretJudgeNotPerformed(judgedQuestion, backendOutput.situation());
            }
            InterpretSentenceResult result = new InterpretSentenceResult();
            updateJudgeInterpretationResult(result, backendOutput);

            Language lang = getUserLanguageByQuestion(judgedQuestion);
            result.explanation = GenerateErrorTextForScopeObjects.generateErrorExplanation(
                    backendOutput.results(),
                    backendOutput.situation().getDomainModel(),
                    lang
            );
            result.explanation = replaceEnumInExplanation(result.explanation, lang);
            result.explanation.setCurrentDomainLawName("incorrectAnswer");

            result.violations = new ArrayList<>();
            result.correctlyAppliedLaws = new ArrayList<>();
            result.isAnswerCorrect = result.explanation.getRawMessage().isEmpty();
            if(!result.isAnswerCorrect) {
                ViolationEntity v = new ViolationEntity();
                v.setLawName("incorrectAnswer");
                v.setViolationFacts(new ArrayList<>());
                result.violations.add(v);
            } else {
                result.IterationsLeft--;
            }
            return result;
        }

        @Override
        protected InterpretSentenceResult interpretJudgeNotPerformed(
                Question judgedQuestion,
                LearningSituation preparedSituation
        ) {
            InterpretSentenceResult result = new InterpretSentenceResult();
            result.violations = new ArrayList<>();
            result.explanation = new Explanation(
                    Explanation.Type.ERROR,
                    ""
            );
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
            interpretationResult.CountCorrectOptions = 1;
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
                    .stream().filter(objectDef -> hasState(objectDef))
                    .count();
            return unevaluatedCount;
        }

        private boolean hasState(ObjectDef object) {
            if (!object.isInstanceOf("Variable")) {
                return false;
            }
            return !((Boolean) object.getPropertyValue("isEvaluated", Map.of()));
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
}
