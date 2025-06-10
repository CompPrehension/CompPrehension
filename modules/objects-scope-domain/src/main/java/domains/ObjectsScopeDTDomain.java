package domains;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.definition.ObjectDef;
import its.model.definition.ObjectRef;
import its.model.definition.rdf.DomainRDFFiller;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import its.reasoner.nodes.DecisionTreeTrace;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.domains.DecisionTreeReasoningDomain;
import org.vstu.compprehension.models.businesslogic.domains.DecisionTreeSupQuestionHelper;
import helpers.GenerateErrorTextForScopeObjects;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.*;
import java.util.stream.Collectors;

import static its.model.definition.build.DomainBuilderUtils.newVariable;
import static its.model.definition.build.DomainBuilderUtils.setBoolProperty;
import static org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph.factsListDeepCopy;

@Log4j2
public class ObjectsScopeDTDomain extends DecisionTreeReasoningDomain {

    static final String END_ANSWER = "EndObject";

    static final String LIFE_TIME = "LifeTime";

    static final String OBJECT_VISIBILITY = "ObjectVisibility";

    static final String OBJECTS_VISIBILITY = "ObjectsVisibility";

    static final String RESOURCES_LOCATION = "domains/";

    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "objects-scope";

    protected final LocalizationService localizationService;

    protected final QuestionBank qMetaStorage;

    static final String MESSAGE_PREFIX = "objscope_";

    private static final HashMap<String, Tag> tags = new HashMap<>() {{
        put("C++", new Tag("C++", 2L));  	// (2 ^ 1)
    }};

    private static final String OBJECT_LIFE_TIME_DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "objects-scope-domain-model/life-time-of-objects-domain-model/";

    private static final String OBJECT_VISIBILITY_DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "objects-scope-domain-model/object-visibility-domain-model/";

    private static final String OBJECTS_VISIBILITY_DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "objects-scope-domain-model/objects-visibility-in-line-domain-model/";

    private final DomainSolvingModel domainLifeTimeSolvingModel = new DomainSolvingModel(
            this.getClass().getClassLoader().getResource(OBJECT_LIFE_TIME_DOMAIN_MODEL_LOCATION),
            DomainSolvingModel.BuildMethod.DICT_RDF
    ).validate();

    private final DomainSolvingModel domainObjectVisibilitySolvingModel = new DomainSolvingModel(
            this.getClass().getClassLoader().getResource(OBJECT_VISIBILITY_DOMAIN_MODEL_LOCATION),
            DomainSolvingModel.BuildMethod.DICT_RDF
    ).validate();

    private final DomainSolvingModel domainObjectsVisibilityInLineSolvingModel = new DomainSolvingModel(
            this.getClass().getClassLoader().getResource(OBJECTS_VISIBILITY_DOMAIN_MODEL_LOCATION),
            DomainSolvingModel.BuildMethod.DICT_RDF
    ).validate();

    public ObjectsScopeDTDomain(
            DomainEntity domainEntity,
            LocalizationService localizationService,
            RandomProvider randomProvider,
            QuestionBank qMetaStorage
    ) {
        super(domainEntity, randomProvider, null);

        this.localizationService = localizationService;
        this.qMetaStorage = qMetaStorage;
        positiveLaws = new HashMap<>();
        negativeLaws = new HashMap<>();
        this.setBackendInterface(new DecisionTreeInterface());

        fillConcepts();
        fillSkills();
    }

    private void fillConcepts() {
        concepts = new HashMap<>();

        int flags = Concept.FLAG_VISIBLE_TO_TEACHER; //для всех стейджей
        int flagsAll = Concept.FLAG_VISIBLE_TO_TEACHER | Concept.FLAG_TARGET_ENABLED; //для конкретных задач в одном стейдже
        addConcepts(List.of(
                new Concept("default_variable", List.of(), flagsAll),
                new Concept("class_property", List.of(), flagsAll),
                new Concept("cycle", List.of(), flagsAll),
                new Concept("recursive_function", List.of(), flagsAll),
                new Concept("if/else", List.of(), flagsAll),
                new Concept("structure", List.of(), flagsAll),
                new Concept("enum", List.of(), flagsAll),
                new Concept("namespace", List.of(), flagsAll),
                new Concept("function", List.of(), flagsAll)
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
        addSkill("global_and_static_variable", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("before_variable_declaration", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("outside_existence_of_variable", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("common_visibility_area", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("variable_access_modifier", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("variable_overlapping", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("visibility_of_non_static_variable_in_static_area", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("visibility_of_variable_that_is_after_and_it_is_not_class", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("visibility_of_variable_that_is_after_and_it_is_class", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("global_variable_visibility", Skill.FLAG_VISIBLE_TO_TEACHER);
        addSkill("static_variable_visibility", Skill.FLAG_VISIBLE_TO_TEACHER);


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
        return localizationService.getMessage("objscope_text.display_name", language);
    }

    @Nullable
    @Override
    public String getDescription(Language language) {
        return localizationService.getMessage("objscope_text.description", language);
    }

    @Override
    public Collection<Fact> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
        System.out.println("responseToFacts");
        if (questionDomainType.equals(LIFE_TIME)) {
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
        } else if (questionDomainType.equals(OBJECT_VISIBILITY)) {
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
        } else if(questionDomainType.equals(OBJECTS_VISIBILITY)) {
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
        throw new NotImplementedException();
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
                .showTrace(false)
                .multipleSelectionEnabled(false)
                .showSupplementaryQuestions(true)
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

        if(Objects.requireNonNull(q.getQuestionType()) == QuestionType.ORDER) {
            entity.setQuestionText(getLocalizedQuestionText(text, userLanguage) + this.ExpressionToHtml(entity.getAnswerObjects(), q.getQuestionDomainType(), userLanguage));
            entity.setOptions(orderQuestionOptions);
            return new Question(entity, this);
        }
        throw new UnsupportedOperationException("Unknown type in ObjectsScopeDTDomain::makeQuestion: " + q.getQuestionType());
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

    public String ExpressionToHtml(List<AnswerObjectEntity> answers, String questionDomainType, Language userLanguage) {
        if(questionDomainType.equals(LIFE_TIME)) {
            StringBuilder sb = new StringBuilder();
            sb.append("<div class='comp-ph-expr trace'>");
            int index = 1;
            sb.append("<p style='height: 400px; overflow: scroll;'>");
            for (AnswerObjectEntity answer : answers) {
                if(answer.getDomainInfo().equals(END_ANSWER)) {
                    continue;
                }
                sb.append("<span ")
                        .append("id='answer_")
                        .append(answer.getAnswerId())
                        .append("' class='comp-ph-expr-op-btn'")
                        .append(">")
                        .append(index++).append(". ")
                        .append(answer.getHyperText())
                        .append("</span><br/>");
            }
            sb.append("</p>");
            sb.append("<button ")
                    .append("id='answer_")
                    .append(answers.getLast().getAnswerId())
                    .append("' class='btn comp-ph-complete-btn data-comp-ph-value'>")
                    .append(getMessage("end_answer", userLanguage))
                    .append("</button>");
            sb.append("</div>");
            return QuestionTextToHtml(sb.toString(), userLanguage);
        } else if (questionDomainType.equals(OBJECT_VISIBILITY)) {
            StringBuilder sb = new StringBuilder();
            sb.append("<p class='comp-ph-expr'>");
            int index = 1;
            for (AnswerObjectEntity answer : answers) {
                if(answer.getDomainInfo().equals(END_ANSWER)) {
                    continue;
                }
                if(answer.getDomainInfo().isEmpty()) {
                    sb.append("<span ")
                            .append("class='comp-ph-expr-const'")
                            .append(">")
                            .append(index++).append(". ")
                            .append(answer.getHyperText())
                            .append("</span><br/>");
                } else {
                    sb.append("<span ")
                            .append("id='answer_")
                            .append(answer.getAnswerId())
                            .append("' class='comp-ph-expr-op-btn'")
                            .append(">")
                            .append(index++).append(". ")
                            .append(answer.getHyperText())
                            .append("</span><br/>");
                }
            }
            sb.append("<button ")
                    .append("id='answer_")
                    .append(answers.getLast().getAnswerId())
                    .append("' class='btn comp-ph-complete-btn data-comp-ph-value'>")
                    .append(getMessage("end_answer", userLanguage))
                    .append("</button>");
            sb.append("</p>");

            return QuestionTextToHtml(sb.toString(), userLanguage);
        } else if(questionDomainType.equals(OBJECTS_VISIBILITY)) {
            StringBuilder sb = new StringBuilder();
            sb.append("<p class='comp-ph-expr'>");
            int index = 1;
            for (AnswerObjectEntity answer : answers) {
                if(answer.getDomainInfo().equals(END_ANSWER)) {
                    continue;
                }
                if(answer.getDomainInfo().isEmpty()) {
                    sb.append("<span ")
                            .append("class='comp-ph-expr-const'")
                            .append(">")
                            .append(index++).append(". ")
                            .append(answer.getHyperText())
                            .append("</span><br/>");
                } else {
                    sb.append("<span ")
                            .append("id='answer_")
                            .append(answer.getAnswerId())
                            .append("' class='comp-ph-expr-op-btn'")
                            .append(">")
                            .append(index++).append(". ")
                            .append(answer.getHyperText())
                            .append("</span><br/>");
                }
            }
            return QuestionTextToHtml(sb.toString(), userLanguage);
        }
        return "";
    }

    private String QuestionTextToHtml(String text, Language language) {
        StringBuilder sb = new StringBuilder(text
                .replaceAll("\\*", "&#8727")
                .replaceAll("\\n", "<br>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                .replaceAll("text.question_program", getMessage("text.question_program", language))
                .replaceAll("text.question_start", getMessage("text.question_start", language))
                .replaceAll("text.question_end_body", getMessage("text.question_end_body", language))
                .replaceAll("text.question_end", getMessage("text.question_end", language))
                .replaceAll("text.question_function", getMessage("text.question_function", language))
                .replaceAll("text.question_condition", getMessage("text.question_condition", language))
                .replaceAll("text.question_evaluation", getMessage("text.question_evaluation", language))
                .replaceAll("text.question_time", getMessage("text.question_time", language))
                .replaceAll("text.question_performed", getMessage("text.question_performed", language))
                .replaceAll("text.question_iteration", getMessage("text.question_iteration", language))
                .replaceAll("text.question_cycle", getMessage("text.question_cycle", language))
                .replaceAll("text.question_body", getMessage("text.question_body", language))
                .replaceAll("text.question_declaration", getMessage("text.question_declaration", language))
                .replaceAll("text.question_class", getMessage("text.question_class", language))
                .replaceAll("text.question_method", getMessage("text.question_method", language))
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

        if(q.getQuestionDomainType().equals(LIFE_TIME)) {
            DomainModel situationModel = factsToDomainModel(q.getQuestionData().getStatementFacts(), domainLifeTimeSolvingModel);
            for (ResponseEntity response : responses) {
                if(!response.getLeftAnswerObject().getDomainInfo().equals(END_ANSWER)) {
                    val step = situationModel.getDomainModel().getObjects().get(response.getLeftAnswerObject().getDomainInfo());
                    setBoolProperty(step, "isEvaluated", true);
                }
            }
            List<ObjectDef> objects = situationModel.getDomainModel().getObjects()
                    .stream().filter(objectDef ->
                            objectDef.isInstanceOf("Step") && !((Boolean) objectDef.getPropertyValue("isEvaluated", Map.of()))
                    ).toList();
            // Проверить в ризонере все возможные варианты интеракций и понять, какая из них правильная и выдать подсказку
            for(ObjectDef object : objects) {
                newVariable(situationModel, "step", object.getName());
                LearningSituation learningSituation = new LearningSituation(
                        situationModel,
                        LearningSituation.collectDecisionTreeVariables(situationModel)
                );

                DecisionTreeTrace decisionTreeTrace = DecisionTreeReasoner.solve(
                        domainLifeTimeSolvingModel.getDecisionTree(),
                        learningSituation
                );

                Explanation hintExplanation = GenerateErrorTextForScopeObjects.generateHintExplanation(decisionTreeTrace, learningSituation.getDomainModel(), lang);

                if(!hintExplanation.getChildren().getFirst().getRawMessage().isEmpty()) {
                    AnswerObjectEntity answer = q.getAnswerObjects().stream()
                            .filter(entity -> object.getName().equals(entity.getDomainInfo()))
                            .findFirst()
                            .orElse(null);
                    CorrectAnswer correctAnswer = new CorrectAnswer();
                    correctAnswer.answers = List.of(new CorrectAnswer.Response(answer, answer));
                    correctAnswer.question = q.getQuestionData();
                    correctAnswer.lawName = null;
                    correctAnswer.skillName = null;
                    correctAnswer.explanation = hintExplanation;
                    return correctAnswer;
                }
                situationModel.getVariables().remove("step");
            }
        } else if (q.getQuestionDomainType().equals(OBJECT_VISIBILITY)) {
            DomainModel situationModel = factsToDomainModel(q.getQuestionData().getStatementFacts(), domainObjectVisibilitySolvingModel);
            for (ResponseEntity response : responses) {
                if(!response.getLeftAnswerObject().getDomainInfo().equals(END_ANSWER)) {
                    val line = situationModel.getDomainModel().getObjects().get(response.getLeftAnswerObject().getDomainInfo());
                    setBoolProperty(line, "isEvaluated", true);
                }
            }
            List<ObjectDef> objects = situationModel.getDomainModel().getObjects()
                    .stream().filter(objectDef ->
                            objectDef.isInstanceOf("Line") && !((Boolean) objectDef.getPropertyValue("isEvaluated", Map.of()))
                    ).toList();
            // Проверить в ризонере все возможные варианты интеракций и понять, какая из них правильная и выдать подсказку
            for(ObjectDef object : objects) {
                newVariable(situationModel, "usageLine", object.getName());
                LearningSituation learningSituation = new LearningSituation(
                        situationModel,
                        LearningSituation.collectDecisionTreeVariables(situationModel)
                );

                DecisionTreeTrace decisionTreeTrace = DecisionTreeReasoner.solve(
                        domainObjectVisibilitySolvingModel.getDecisionTree(),
                        learningSituation
                );
                Explanation hintExplanation = GenerateErrorTextForScopeObjects.generateHintExplanation(decisionTreeTrace, learningSituation.getDomainModel(), lang);

                if(!hintExplanation.getChildren().getFirst().getRawMessage().isEmpty()) {
                    AnswerObjectEntity answer = q.getAnswerObjects().stream()
                            .filter(entity -> object.getName().equals(entity.getDomainInfo()))
                            .findFirst()
                            .orElse(null);
                    CorrectAnswer correctAnswer = new CorrectAnswer();
                    correctAnswer.answers = List.of(new CorrectAnswer.Response(answer, answer));
                    correctAnswer.question = q.getQuestionData();
                    correctAnswer.lawName = null;
                    correctAnswer.skillName = null;
                    correctAnswer.explanation = hintExplanation;
                    return correctAnswer;
                }
                situationModel.getVariables().remove("usageLine");
            }
        } else if(q.getQuestionDomainType().equals(OBJECTS_VISIBILITY)) {
            DomainModel situationModel = factsToDomainModel(q.getQuestionData().getStatementFacts(), domainObjectsVisibilityInLineSolvingModel);
            for (ResponseEntity response : responses) {
                val context = situationModel.getDomainModel().getObjects().get(response.getLeftAnswerObject().getDomainInfo());
                setBoolProperty(context, "isEvaluated", true);
            }
            List<ObjectDef> objects = situationModel.getDomainModel().getObjects()
                    .stream().filter(objectDef ->
                            objectDef.isInstanceOf("Context") && !objectDef.isInstanceOf("DataStructure") && !((Boolean) objectDef.getPropertyValue("isEvaluated", Map.of()))
                    ).toList();
            // Проверить в ризонере все возможные варианты интеракций и понять, какая из них правильная и выдать подсказку
            for(ObjectDef object : objects) {
                newVariable(situationModel, "currentCt", object.getName());
                LearningSituation learningSituation = new LearningSituation(
                        situationModel,
                        LearningSituation.collectDecisionTreeVariables(situationModel)
                );

                DecisionTreeTrace decisionTreeTrace = DecisionTreeReasoner.solve(
                        domainObjectsVisibilityInLineSolvingModel.getDecisionTree(),
                        learningSituation
                );
                Explanation hintExplanation = GenerateErrorTextForScopeObjects.generateHintExplanation(decisionTreeTrace, learningSituation.getDomainModel(), lang);

                if(!hintExplanation.getChildren().getFirst().getRawMessage().isEmpty()) {
                    AnswerObjectEntity answer = q.getAnswerObjects().stream()
                            .filter(entity -> object.getName().equals(entity.getDomainInfo()))
                            .findFirst()
                            .orElse(null);
                    CorrectAnswer correctAnswer = new CorrectAnswer();
                    correctAnswer.answers = List.of(new CorrectAnswer.Response(answer, answer));
                    correctAnswer.question = q.getQuestionData();
                    correctAnswer.lawName = null;
                    correctAnswer.skillName = null;
                    correctAnswer.explanation = hintExplanation;
                    return correctAnswer;
                }
                situationModel.getVariables().remove("currentCt");
            }
        }
        AnswerObjectEntity answer = q.getAnswerObjects().stream()
                .filter(entity -> END_ANSWER.equals(entity.getDomainInfo()))
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

    @Override
    protected List<Question> getQuestionTemplates() {
        throw new NotImplementedException();
    }


    //-----Суждение вопросов и подобное ------

    @Override
    public Set<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        throw new NotImplementedException();
    }

    @Override
    public Set<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        throw new NotImplementedException();
    }

    @Override
    public Collection<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        throw new UnsupportedOperationException("no Laws are used for " + this.getClass().getSimpleName());
    }

    @Override
    public Collection<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        throw new NotImplementedException();
    }


    //-----------Объяснения---------------
    @Override
    public InterpretSentenceResult interpretSentence(Collection<Fact> violations) {
        throw new NotImplementedException();
    }

    @Override
    public Explanation makeExplanation(List<ViolationEntity> mistakes, FeedbackType feedbackType, Language lang) {
        throw new NotImplementedException();
    }


    private HashMap<String, Long> _getConceptsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(9);
        name2bit.put("default_variable", 0x1L);  		// (1)
        name2bit.put("class_property", 0x2L);  			// (2)
        name2bit.put("cycle", 0x4L);  			// (4)
        name2bit.put("recursive_function", 0x8L);  		// (8)
        name2bit.put("if/else", 0x10L);  	// (16)
        name2bit.put("structure", 0x20L);    // (32)
        name2bit.put("enum", 0x40L);  // (64)
        name2bit.put("namespace", 0x80L);  // (128)
        name2bit.put("function", 0x100L);  // (256)
        return name2bit;
    }

    private HashMap<String, Long> _getSkillsName2bit() {
        HashMap<String, Long> name2bit = new HashMap<>(11);
        name2bit.put("global_and_static_variable", 0x1L);  	// (1)
        name2bit.put("before_variable_declaration", 0x2L);  	// (2)
        name2bit.put("outside_existence_of_variable", 0x4L);  	// (4)

        name2bit.put("common_visibility_area", 0x8L);  	// (8)
        name2bit.put("variable_access_modifier", 0x10L);  	// (16)
        name2bit.put("variable_overlapping", 0x20L);  	// (32)
        name2bit.put("visibility_of_non_static_variable_in_static_area", 0x40L);  	// (64)
        name2bit.put("visibility_of_variable_that_is_after_and_it_is_not_class", 0x80L);  	// (128)
        name2bit.put("visibility_of_variable_that_is_after_and_it_is_class", 0x100L);  	// (256)
        name2bit.put("global_variable_visibility", 0x200L);  	// (512)
        name2bit.put("static_variable_visibility", 0x400L);  	// (1024)
        return name2bit;
    }

    @Override
    public QuestionRequest ensureQuestionRequestValid(QuestionRequest questionRequest) {
        return questionRequest.toBuilder()
                .stepsMin(1)
                .stepsMax(30)
                .build();
    }

    private DomainModel mainQuestionToModel(InteractionEntity lastMainQuestionInteraction) {
        Question question = new Question(lastMainQuestionInteraction.getQuestion(), this);

        DomainModel situationModel = factsToDomainModel(question.getQuestionData().getStatementFacts(), domainLifeTimeSolvingModel);
        ResponseEntity lastResponse = lastMainQuestionInteraction.getResponses().getLast();
        for (ResponseEntity response : lastMainQuestionInteraction.getResponses()) {
            if(response != lastResponse && !response.getLeftAnswerObject().getDomainInfo().equals(END_ANSWER)) {
                val step = situationModel.getDomainModel().getObjects().get(response.getLeftAnswerObject().getDomainInfo());
                setBoolProperty(step, "isEvaluated", true);
            }
        }
        if(!lastResponse.getLeftAnswerObject().getDomainInfo().equals(END_ANSWER)) {
            newVariable(situationModel, "step", lastResponse.getLeftAnswerObject().getDomainInfo());
        }
        return situationModel;
    }

    private DomainModel factsToDomainModel(Collection<BackendFactEntity> factEntities, DomainSolvingModel domainSolvingModel) {
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
    private final DecisionTreeSupQuestionHelper dtSupplementaryQuestionHelper = new DecisionTreeSupQuestionHelper(
            this,
            domainLifeTimeSolvingModel,
            this::mainQuestionToModel
    );

    @Override
    public String getDefaultQuestionType(boolean supplementary) {
        return "Supplementary";
    }

    @Override
    public SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionEntity sourceQuestion, ViolationEntity violation, Language lang) {
            return dtSupplementaryQuestionHelper.makeSupplementaryQuestion(sourceQuestion, lang);
    }

    @Override
    public SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(Question question, SupplementaryStepEntity supplementaryStep, List<ResponseEntity> responses) {
            return dtSupplementaryQuestionHelper.judgeSupplementaryQuestion(supplementaryStep, responses);
    }

    @Override
    public boolean needSupplementaryQuestion(ViolationEntity violation) {
        return violation.getLawName().equals("incorrectStep");
    }

    private class DecisionTreeInterface extends DecisionTreeReasonerBackend.Interface {
        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForJudge(
                Question question,
                List<ResponseEntity> responses,
                List<Tag> tags
        ) {
            if(question.getQuestionDomainType().equals(LIFE_TIME)) {
                DomainModel situationModel = factsToDomainModel(question.getQuestionData().getStatementFacts(), domainLifeTimeSolvingModel);
                ResponseEntity lastResponse = responses.getLast();
                for (ResponseEntity response : responses) {
                    if(response != lastResponse && !response.getLeftAnswerObject().getDomainInfo().equals(END_ANSWER)) {
                        val step = situationModel.getDomainModel().getObjects().get(response.getLeftAnswerObject().getDomainInfo());
                        setBoolProperty(step, "isEvaluated", true);
                    }
                }
                if(!lastResponse.getLeftAnswerObject().getDomainInfo().equals(END_ANSWER)) {
                    newVariable(situationModel, "step", lastResponse.getLeftAnswerObject().getDomainInfo());
                }

                return new DecisionTreeReasonerBackend.Input(
                        situationModel,
                        domainLifeTimeSolvingModel.getDecisionTree()
                );
            } else if (question.getQuestionDomainType().equals(OBJECT_VISIBILITY)) {
                DomainModel situationModel = factsToDomainModel(question.getQuestionData().getStatementFacts(), domainObjectVisibilitySolvingModel);
                ResponseEntity lastResponse = responses.getLast();
                for (ResponseEntity response : responses) {
                    if(response != lastResponse && !response.getLeftAnswerObject().getDomainInfo().equals(END_ANSWER)) {
                        val line = situationModel.getDomainModel().getObjects().get(response.getLeftAnswerObject().getDomainInfo());
                        setBoolProperty(line, "isEvaluated", true);
                    }
                }
                if(!lastResponse.getLeftAnswerObject().getDomainInfo().equals(END_ANSWER)) {
                    newVariable(situationModel, "usageLine", lastResponse.getLeftAnswerObject().getDomainInfo());
                }

                return new DecisionTreeReasonerBackend.Input(
                        situationModel,
                        domainObjectVisibilitySolvingModel.getDecisionTree()
                );
            } else if(question.getQuestionDomainType().equals(OBJECTS_VISIBILITY)) {
                DomainModel situationModel = factsToDomainModel(question.getQuestionData().getStatementFacts(), domainObjectsVisibilityInLineSolvingModel);
                ResponseEntity lastResponse = responses.getLast();
                for (ResponseEntity response : responses) {
                    if(response != lastResponse) {
                        val context = situationModel.getDomainModel().getObjects().get(response.getLeftAnswerObject().getDomainInfo());
                        setBoolProperty(context, "isEvaluated", true);
                    }
                }
                newVariable(situationModel, "currentCt", lastResponse.getLeftAnswerObject().getDomainInfo());

                return new DecisionTreeReasonerBackend.Input(
                        situationModel,
                        domainObjectsVisibilityInLineSolvingModel.getDecisionTree()
                );
            }
            return null;
        }

        @Override
        public InterpretSentenceResult interpretJudgeOutput(Question judgedQuestion, DecisionTreeReasonerBackend.Output backendOutput) {
            if(!backendOutput.isReasoningDone()){
                return interpretJudgeNotPerformed(judgedQuestion, backendOutput.situation());
            }
            InterpretSentenceResult result = new InterpretSentenceResult();
            updateInterpretationResult(judgedQuestion.getQuestionDomainType(), result, backendOutput.situation());

            Language lang = getUserLanguageByQuestion(judgedQuestion);
            result.explanation = GenerateErrorTextForScopeObjects.generateErrorExplanation(
                    backendOutput.results(),
                    backendOutput.situation().getDomainModel(),
                    lang
            );
            result.violations = new ArrayList<>();
            result.correctlyAppliedLaws = new ArrayList<>();
            result.isAnswerCorrect = result.explanation.getRawMessage().isEmpty();

            if(judgedQuestion.getQuestionDomainType().equals(LIFE_TIME)) {
                result.explanation.setCurrentDomainLawName("incorrectStep");
                if(!result.isAnswerCorrect) {
                    ViolationEntity v = new ViolationEntity();
                    v.setLawName("incorrectStep");
                    v.setViolationFacts(new ArrayList<>());
                    result.violations.add(v);
                }
            } else if (judgedQuestion.getQuestionDomainType().equals(OBJECT_VISIBILITY)) {
                result.explanation.setCurrentDomainLawName("incorrectLine");
                if(!result.isAnswerCorrect) {
                    ViolationEntity v = new ViolationEntity();
                    v.setLawName("incorrectLine");
                    v.setViolationFacts(new ArrayList<>());
                    result.violations.add(v);
                }
            } else if(judgedQuestion.getQuestionDomainType().equals(OBJECTS_VISIBILITY)) {
                result.explanation.setCurrentDomainLawName("incorrectVariable");
                if(!result.isAnswerCorrect) {
                    ViolationEntity v = new ViolationEntity();
                    v.setLawName("incorrectVariable");
                    v.setViolationFacts(new ArrayList<>());
                    result.violations.add(v);
                }
            }
            return result;
        }

        @Override
        protected InterpretSentenceResult interpretJudgeNotPerformed(
                Question judgedQuestion,
                LearningSituation preparedSituation
        ) {
            if(judgedQuestion.getQuestionDomainType().equals(LIFE_TIME)) {
                DecisionTreeTrace decisionTreeTrace = DecisionTreeReasoner.solve(
                        domainLifeTimeSolvingModel.decisionTree("all"),
                        preparedSituation
                );

                InterpretSentenceResult result = new InterpretSentenceResult();
                result.violations = new ArrayList<>();
                result.explanation = GenerateErrorTextForScopeObjects.generateErrorExplanation(
                        decisionTreeTrace,
                        preparedSituation.getDomainModel(),
                        getUserLanguageByQuestion(judgedQuestion)
                );
                result.explanation.setCurrentDomainLawName("incorrectSteps");

                updateInterpretationResult(judgedQuestion.getQuestionDomainType(), result, preparedSituation);
                if(!result.explanation.getRawMessage().isEmpty()) {
                    ViolationEntity v = new ViolationEntity();
                    v.setLawName("incorrectSteps");
                    v.setViolationFacts(new ArrayList<>());
                    result.violations.add(v);
                }
                return result;
            } else if (judgedQuestion.getQuestionDomainType().equals(OBJECT_VISIBILITY)) {
                DecisionTreeTrace decisionTreeTrace = DecisionTreeReasoner.solve(
                        domainObjectVisibilitySolvingModel.decisionTree("all"),
                        preparedSituation
                );

                InterpretSentenceResult result = new InterpretSentenceResult();
                result.violations = new ArrayList<>();
                result.explanation = GenerateErrorTextForScopeObjects.generateErrorExplanation(
                        decisionTreeTrace,
                        preparedSituation.getDomainModel(),
                        getUserLanguageByQuestion(judgedQuestion)
                );
                result.explanation.setCurrentDomainLawName("incorrectLines");

                updateInterpretationResult(judgedQuestion.getQuestionDomainType(), result, preparedSituation);
                if(!result.explanation.getRawMessage().isEmpty()) {
                    ViolationEntity v = new ViolationEntity();
                    v.setLawName("incorrectLines");
                    v.setViolationFacts(new ArrayList<>());
                    result.violations.add(v);
                }
                return result;
            } else if(judgedQuestion.getQuestionDomainType().equals(OBJECTS_VISIBILITY)) {
                return null;
            }
            return null;
        }

        @Override
        protected void updateJudgeInterpretationResult(
                InterpretSentenceResult interpretationResult,
                DecisionTreeReasonerBackend.Output backendOutput
        ) {}

        private void updateInterpretationResult(
                String questionDomainType,
                InterpretSentenceResult interpretationResult,
                LearningSituation situation
        ) {
            interpretationResult.CountCorrectOptions = 1;
            if(questionDomainType.equals(LIFE_TIME)) {
                interpretationResult.IterationsLeft = calculateLeftInteractionsInLifeTime(situation);
            } else if (questionDomainType.equals(OBJECT_VISIBILITY)) {
                interpretationResult.IterationsLeft = calculateLeftInteractionsObjectVisibility(situation);
            } else if (questionDomainType.equals(OBJECTS_VISIBILITY)) {
                interpretationResult.IterationsLeft = calculateLeftInteractionsObjectsVisibility(situation);
            }

            if (interpretationResult.IterationsLeft == 0) {
                // Достигли полного завершения задачи.
                // Ошибок уже быть не может — сбросим их все.
                interpretationResult.isAnswerCorrect = true;
                interpretationResult.violations = List.of();
                interpretationResult.explanation = Explanation.empty(Explanation.Type.HINT);
            }
        }

        public int calculateLeftInteractionsInLifeTime(LearningSituation situation) {
            int unevaluatedCount = (int) situation.getDomainModel().getObjects()
                    .stream().filter(objectDef -> stepHasStateIsUnevaluated(objectDef))
                    .count();

            // Эвристика, может давать сбои: если студент нажал "ничего больше не выполнится",
            // то дерево не запускалось и его переменные не задавались.
            ObjectRef endEvaluationClicked = situation.getDecisionTreeVariables().get("step");

            // Потребовать нажать кнопку "ничего больше не выполнится", если есть корректно опущенные операторы (чтобы проверить, понимает ли студент это).
            int oneMoreStepForEndEvaluation = unevaluatedCount == 0 && endEvaluationClicked != null ? 1 : 0;

            return unevaluatedCount + oneMoreStepForEndEvaluation;
        }

        public int calculateLeftInteractionsObjectVisibility(LearningSituation situation) {
            int unevaluatedCount = (int) situation.getDomainModel().getObjects()
                    .stream().filter(objectDef -> lineHasStateIsUnevaluated(objectDef))
                    .count();

            // Эвристика, может давать сбои: если студент нажал "ничего больше не выполнится",
            // то дерево не запускалось и его переменные не задавались.
            ObjectRef endEvaluationClicked = situation.getDecisionTreeVariables().get("usageLine");

            // Потребовать нажать кнопку "ничего больше не выполнится", если есть корректно опущенные операторы (чтобы проверить, понимает ли студент это).
            int oneMoreStepForEndEvaluation = unevaluatedCount == 0 && endEvaluationClicked != null ? 1 : 0;

            return unevaluatedCount + oneMoreStepForEndEvaluation;
        }

        public int calculateLeftInteractionsObjectsVisibility(LearningSituation situation) {
            int unevaluatedCount = (int) situation.getDomainModel().getObjects()
                    .stream().filter(objectDef -> contextHasStateIsUnevaluated(objectDef))
                    .count();

            return unevaluatedCount;
        }

        private boolean stepHasStateIsUnevaluated(ObjectDef object) {
            if (!object.isInstanceOf("Step")) {
                return false;
            }
            return !((Boolean) object.getPropertyValue("isEvaluated", Map.of()));
        }

        private boolean lineHasStateIsUnevaluated(ObjectDef object) {
            if (!object.isInstanceOf("Line")) {
                return false;
            }
            return !((Boolean) object.getPropertyValue("isEvaluated", Map.of()));
        }

        private boolean contextHasStateIsUnevaluated(ObjectDef object) {
            if (!object.isInstanceOf("Context") || object.isInstanceOf("DataStructure")) {
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
