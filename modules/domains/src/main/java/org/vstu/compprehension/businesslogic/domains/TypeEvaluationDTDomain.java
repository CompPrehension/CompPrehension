package org.vstu.compprehension.businesslogic.domains;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.definition.ObjectDef;
import its.model.definition.ParamsValues;
import its.model.definition.RelationshipLinkStatement;
import its.model.definition.VariableDef;
import its.model.definition.loqi.DomainLoqiBuilder;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.ConceptsBuilder;
import org.vstu.compprehension.businesslogic.DomainItemFlag;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.businesslogic.HyperText;
import org.vstu.compprehension.businesslogic.Laws;
import org.vstu.compprehension.businesslogic.NegativeLaw;
import org.vstu.compprehension.businesslogic.PositiveLaw;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.SkillsBuilder;
import org.vstu.compprehension.businesslogic.SupplementaryFeedbackGenerationResult;
import org.vstu.compprehension.businesslogic.SupplementaryResponseGenerationResult;
import org.vstu.compprehension.businesslogic.SupplementaryStepContext;
import org.vstu.compprehension.businesslogic.Tag;
import org.vstu.compprehension.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.businesslogic.backend.Fact;
import org.vstu.compprehension.businesslogic.domains.helpers.DomainSolvingModelLoader;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.BackendFactData;
import org.vstu.compprehension.data.question.GeneratedQuestionData;
import org.vstu.compprehension.data.question.QuestionContentData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.FeedbackType;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.SearchDirections;
import org.vstu.compprehension.services.LocalizationService;
import org.vstu.compprehension.services.RandomProvider;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Определение типа результата каждой части выражения. */
public class TypeEvaluationDTDomain extends DecisionTreeReasoningDomain {
    public static final String DOMAIN_ID = "type_eval_dt";
    static final String RESOURCES_LOCATION = "domains/";
    public static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "type-evaluation-domain-model/";
    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "type-evaluation";
    static final String MESSAGE_PREFIX = "typeeval_text.";

    private static final String SITUATION_FACT_VERB = "hasLoqi";
    // Эталонный тип операции хранится в метаданных: выражения дерева метаданные не читают, поэтому подсмотреть его нельзя.
    private static final String EXPECTED_TYPE = "expectedType";
    // Значение операции в записи языка вопроса. Необязательно: для статически типизированных языков или значений,
    // зависящих от ввода пользователя, его нет.
    private static final String VALUE = "value";
    private static final String LOCALIZED_NAME = "localizedName";
    private static final String SOURCE_TEXT = "text";
    private static final String EVALUATION_ERROR_CLASS = "EvaluationError";
    private static final String HAS_TYPE = "hasType";
    private static final String HAS_OPERAND = "hasOperand";
    private static final String OPERATION_VARIABLE = "E";
    private static final String ANSWER_VARIABLE = "T";
    public static final String EVALUATION_ORDER_VIOLATION = "evaluation_order";

    private static final Map<String, Tag> TAGS = Map.of("Python", new Tag("Python", 1L));
    // Фронтенд не знает оформления вопросов домена, поэтому стили приходят вместе с текстом вопроса.
    private static final String QUESTION_STYLES = "<style>" + readResource(RESOURCES_LOCATION + "type-evaluation-question.css") + "</style>";

    private final DomainSolvingModel domainSolvingModel = DomainSolvingModelLoader.loadFromClasspath(
            getClass().getClassLoader(),
            DOMAIN_MODEL_LOCATION,
            DomainSolvingModel.BuildMethod.LOQI).validate(false);
    private final DecisionTreeInterface backendInterface = new DecisionTreeInterface();
    private final LocalizationService localizationService;
    private final QuestionBank qMetaStorage;

    public TypeEvaluationDTDomain(RandomProvider randomProvider,
                                  LocalizationService localizationService,
                                  QuestionBank qMetaStorage) {
        super(DOMAIN_ID, randomProvider, new DomainStructure(buildConcepts(), buildSkills(), Laws.empty()));
        this.localizationService = localizationService;
        this.qMetaStorage = qMetaStorage;
    }

    private static Map<String, Skill> buildSkills() {
        var b = new SkillsBuilder();
        var visible = EnumSet.of(DomainItemFlag.VISIBLE_TO_TEACHER);

        b.add("true_division_result", 0x1L, visible);
        b.add("numeric_result_type", 0x2L, visible);
        b.add("sequence_operation_applicability", 0x4L, visible);
        b.add("negation_result", 0x8L, visible);
        b.add("boolean_logical_result", 0x10L, visible);
        b.add("logical_returned_operand", 0x20L, visible);
        b.add("equality_comparison", 0x40L, visible);
        b.add("ordering_comparison", 0x80L, visible);
        b.add("membership_test", 0x100L, visible);
        b.add("mapping_value_access", 0x200L, visible);
        b.add("indexing_applicability", 0x400L, visible);
        b.add("indexed_element_type", 0x800L, visible);
        b.add("slice_result", 0x1000L, visible);
        b.add("call_fixed_result", 0x2000L, visible);
        b.add("length_applicability", 0x4000L, visible);
        b.add("conversion_applicability", 0x8000L, visible);
        b.add("operand_identification", 0x10000L, visible);

        return b.build();
    }

    private static Map<String, Concept> buildConcepts() {
        var b = new ConceptsBuilder();
        var flags = EnumSet.of(DomainItemFlag.VISIBLE_TO_TEACHER, DomainItemFlag.TARGET_ENABLED);

        Concept operations = b.add("operations");
        b.add("arithmetic", 0x1L, List.of(operations), flags);
        b.add("division", 0x2L, List.of(operations), flags);
        b.add("logical", 0x4L, List.of(operations), flags);
        b.add("truthiness", 0x8L, List.of(operations), flags);
        b.add("comparison", 0x10L, List.of(operations), flags);
        b.add("membership", 0x20L, List.of(operations), flags);
        b.add("indexing", 0x40L, List.of(operations), flags);
        b.add("slicing", 0x80L, List.of(operations), flags);
        b.add("function_call", 0x100L, List.of(operations), flags);

        Concept types = b.add("types");
        b.add("numbers", 0x200L, List.of(types), flags);
        b.add("strings", 0x400L, List.of(types), flags);
        b.add("lists", 0x800L, List.of(types), flags);
        b.add("tuples", 0x1000L, List.of(types), flags);
        b.add("dicts", 0x2000L, List.of(types), flags);
        b.add("type_errors", 0x4000L, List.of(types), flags);

        return b.build();
    }

    private class DecisionTreeInterface implements DecisionTreeReasonerBackend.Interface {

        @Override
        public DecisionTreeReasoningDomain getDomain() {
            return TypeEvaluationDTDomain.this;
        }

        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForJudge(QuestionData question,
                                                                           List<? extends AnswerData> responses,
                                                                           List<Tag> tags) {
            var model = prepareQuestionModel(question.getContent());
            var judged = responses.getLast();
            markSolvedOperations(model, responses.subList(0, responses.size() - 1));

            var operation = model.getObjects().get(judged.left().getDomainInfo());
            // Без переменных бэкенд не запускает дерево и спрашивает interpretJudgeNotPerformed.
            if (areOperandsSolved(operation)) {
                model.getVariables().add(new VariableDef(OPERATION_VARIABLE, operation.getName()));
                model.getVariables().add(new VariableDef(ANSWER_VARIABLE, judged.right().getDomainInfo()));
            }
            return new DecisionTreeReasonerBackend.Input(model, domainSolvingModel.getDecisionTree(), domainSolvingModel);
        }

        @Override
        public DecisionTreeReasonerBackend.Input prepareBackendInfoForSolve(QuestionContentData question, List<Tag> tags) {
            return null;
        }

        @Override
        public Judgement interpretJudgeNotPerformed(QuestionData judgedQuestion,
                                                    LearningSituation preparedSituation,
                                                    Language language) {
            var violation = new ViolationData();
            violation.setKnowledgeName(EVALUATION_ORDER_VIOLATION);
            var explanation = Explanation.aggregate(Explanation.Type.ERROR,
                    List.of(new Explanation(Explanation.Type.ERROR, getMessage("operands_first", language))));
            return new Judgement.Verdict(false, explanation, List.of(violation), List.of(),
                    countUnsolvedOperations(preparedSituation.getDomainModel()));
        }

        @Override
        public int countStepsLeft(@NotNull QuestionData judgedQuestion,
                                  @NotNull DecisionTreeReasonerBackend.Output backendOutput,
                                  boolean isAnswerCorrect) {
            var unsolved = countUnsolvedOperations(backendOutput.situation().getDomainModel());
            return isAnswerCorrect ? unsolved - 1 : unsolved;
        }
    }

    private static @NotNull String readResource(@NotNull String location) {
        try (var stream = TypeEvaluationDTDomain.class.getClassLoader().getResourceAsStream(location)) {
            if (stream == null) {
                throw new IllegalStateException("Resource '" + location + "' is not found in classpath");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private @NotNull DomainModel prepareQuestionModel(@NotNull QuestionContentData question) {
        var model = domainSolvingModel.getMergedTagDomain(findModelTag(question.getTags()));
        var situationLoqi = question.getStatementFacts().stream()
                .filter(fact -> fact.getVerb().equals(SITUATION_FACT_VERB))
                .map(BackendFactData::getObject)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Question has no LOQI situation"));
        try (var reader = new StringReader(situationLoqi)) {
            model.addMerge(DomainLoqiBuilder.buildDomain(reader));
        }
        nameObjectsAsStudentSeesThem(model, question.getAnswerObjects());
        return model;
    }

    // Объяснения дерева называют объекты так, как их видит студент: типы и части выражения — подписями вариантов
    // и слотов, а всё, у чего есть текст из кода (метаданные text), — этим текстом.
    private static void nameObjectsAsStudentSeesThem(@NotNull DomainModel model, @NotNull List<AnswerObjectData> answerObjects) {
        for (var answer : answerObjects) {
            var object = model.getObjects().get(answer.getDomainInfo());
            if (object != null) {
                setDisplayName(object, answer.getHyperText());
            }
        }
        for (var object : model.getObjects()) {
            var text = object.getMetadata().getString(SOURCE_TEXT);
            if (text != null) {
                setDisplayName(object, text);
            }
        }
    }

    private static void setDisplayName(@NotNull ObjectDef object, @NotNull String text) {
        var name = "<code>" + escapeHtml(text) + "</code>";
        for (var language : Language.values()) {
            object.getMetadata().add(language.toLocaleString(), LOCALIZED_NAME, name);
        }
    }

    private static @NotNull String escapeHtml(@NotNull String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static @NotNull String findModelTag(@NotNull Collection<String> questionTags) {
        return questionTags.stream()
                .filter(TAGS::containsKey)
                .map(String::toLowerCase)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Question has no language tag: " + questionTags));
    }

    /** Операции, на которые уже дан эталонный ответ, получают свой тип и становятся операндами для родителей. */
    private static void markSolvedOperations(@NotNull DomainModel model, @NotNull List<? extends AnswerData> responses) {
        for (var response : responses) {
            var operation = model.getObjects().get(response.left().getDomainInfo());
            var expectedType = operation.getMetadata().getString(EXPECTED_TYPE);
            if (response.right().getDomainInfo().equals(expectedType)) {
                operation.getRelationshipLinks().add(new RelationshipLinkStatement(
                        operation, HAS_TYPE, List.of(expectedType), ParamsValues.getEMPTY()));
            }
        }
    }

    private static boolean areOperandsSolved(@NotNull ObjectDef operation) {
        return operation.getRelationshipLinks().stream()
                .filter(link -> link.getRelationshipName().equals(HAS_OPERAND))
                .flatMap(link -> link.getObjects().stream())
                .allMatch(TypeEvaluationDTDomain::hasType);
    }

    private static boolean hasType(@NotNull ObjectDef expression) {
        return expression.getRelationshipLinks().stream()
                .anyMatch(link -> link.getRelationshipName().equals(HAS_TYPE));
    }

    private static @NotNull List<ObjectDef> findOperations(@NotNull DomainModel model) {
        return model.getObjects().stream()
                .filter(object -> object.getMetadata().getString(EXPECTED_TYPE) != null)
                .toList();
    }

    private static int countUnsolvedOperations(@NotNull DomainModel model) {
        return (int) findOperations(model).stream().filter(operation -> !hasType(operation)).count();
    }

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(QuestionData q, Language language) {
        var content = q.getContent();
        var model = prepareQuestionModel(content);
        markSolvedOperations(model, q.findLatestCorrectAnswers());

        // Среди готовых к ответу операций берём первую по порядку слотов в тексте.
        var slot = content.getAnswerObjects().stream()
                .filter(answer -> !answer.isRightCol())
                .filter(answer -> {
                    var operation = model.getObjects().get(answer.getDomainInfo());
                    return !hasType(operation) && areOperandsSolved(operation);
                })
                .min(Comparator.comparingInt(AnswerObjectData::getAnswerId))
                .orElse(null);
        if (slot == null) {
            return null;
        }
        var expectedType = model.getObjects().get(slot.getDomainInfo()).getMetadata().getString(EXPECTED_TYPE);
        var typeOption = content.getAnswerObjects().stream()
                .filter(answer -> answer.isRightCol() && answer.getDomainInfo().equals(expectedType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No answer option for type " + expectedType));

        model.getVariables().add(new VariableDef(OPERATION_VARIABLE, slot.getDomainInfo()));
        model.getVariables().add(new VariableDef(ANSWER_VARIABLE, expectedType));
        var situation = new LearningSituation(model, LearningSituation.collectDecisionTreeVariables(model), domainSolvingModel);
        var trace = DecisionTreeReasoner.solve(domainSolvingModel.getDecisionTree(), situation);

        var correctAnswer = new CorrectAnswer();
        correctAnswer.question = q;
        correctAnswer.answers = List.of(new AnswerData.Pair(slot, typeOption));
        correctAnswer.explanation = DecisionTreeReasonerBackend.collectExplanationsFromTrace(
                Explanation.Type.HINT, trace, model, this, language);
        correctAnswer.skillName = DecisionTreeReasonerBackend.nestedTraceElements(trace).stream()
                .map(element -> element.getNode().getMetadata().getString("skill"))
                .filter(skill -> skill != null)
                .distinct()
                .toList();
        return correctAnswer;
    }

    @NotNull
    @Override
    public List<HyperText> getFullSolutionTrace(@NotNull QuestionData question, @NotNull Language language) {
        var content = question.getContent();
        var typeNames = new HashMap<String, String>();
        content.getAnswerObjects().stream()
                .filter(AnswerObjectData::isRightCol)
                .forEach(option -> typeNames.put(option.getDomainInfo(), option.getHyperText()));
        var model = prepareQuestionModel(content);
        return question.findLatestCorrectAnswers().stream()
                .sorted(Comparator.comparingInt(answer -> answer.left().getAnswerId()))
                .map(answer -> {
                    var value = model.getObjects().get(answer.left().getDomainInfo()).getMetadata().getString(VALUE);
                    var isError = model.getObjects().get(answer.right().getDomainInfo()).isInstanceOf(EVALUATION_ERROR_CLASS);
                    var template = getMessage(isError ? "trace.template.error" : value == null ? "trace.template" : "trace.template.value", language);
                    return new HyperText(template
                            .replace("${expression}", answer.left().getHyperText())
                            .replace("${value}", value == null ? "" : escapeHtml(value))
                            .replace("${type}", typeNames.getOrDefault(answer.right().getDomainInfo(), answer.right().getHyperText())));
                })
                .toList();
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
        return getMessage("display_name", language);
    }

    @Nullable
    @Override
    public String getDescription(Language language) {
        return getMessage("description", language);
    }

    @NotNull
    @Override
    public Map<String, Tag> getTags() {
        return TAGS;
    }

    @Override
    public Collection<Fact> responseToFacts(QuestionData question, List<? extends AnswerData> responses) {
        return List.of();
    }

    @Override
    public Collection<Fact> getQuestionStatementFactsWithSchema(QuestionContentData q) {
        return List.of();
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
    public Explanation makeExplanation(List<ViolationData> violations, FeedbackType feedbackType, Language lang) {
        return Explanation.empty(Explanation.Type.ERROR);
    }

    @NotNull
    @Override
    public GeneratedQuestionData makeQuestion(@NotNull QuestionRequest questionRequest,
                                              @Nullable ExerciseOptionsData exerciseOptions,
                                              @NotNull Language userLanguage) {
        int generatorThreshold = exerciseOptions != null && exerciseOptions.getGeneratorThreshold() != null
                ? exerciseOptions.getGeneratorThreshold()
                : 1;
        int additionalQuestions = exerciseOptions != null && exerciseOptions.getGeneratorAdditionalQuestionsToGenerate() != null
                ? exerciseOptions.getGeneratorAdditionalQuestionsToGenerate()
                : 3;
        var found = qMetaStorage.searchQuestions(questionRequest, 1, generatorThreshold, additionalQuestions).getQuestions();
        if (found.isEmpty() && questionRequest.getLawsSearchDirection() == SearchDirections.TO_COMPLEX) {
            var simpler = questionRequest.toBuilder().lawsSearchDirection(SearchDirections.TO_SIMPLE).build();
            found = qMetaStorage.searchQuestions(simpler, 1, generatorThreshold, additionalQuestions).getQuestions();
        }
        if (found.isEmpty()) {
            throw new IllegalStateException("No valid questions found");
        }
        return makeQuestion(found.getFirst(), List.of(), userLanguage);
    }

    @NotNull
    @Override
    public GeneratedQuestionData makeQuestion(@NotNull QuestionMetadataWithData metadata,
                                              @NotNull List<Tag> tags,
                                              @NotNull Language userLang) {
        var result = metadata.getData().toQuestion(this, metadata);
        return result.withContent(result.getContent().toBuilder()
                .questionText(QUESTION_STYLES + getMessage("question_prompt", userLang) + result.getContent().getQuestionText())
                .build());
    }

    @Override
    public QuestionRequest ensureQuestionRequestValid(QuestionRequest questionRequest) {
        return questionRequest.toBuilder()
                .stepsMin(1)
                .stepsMax(15)
                .build();
    }

    @Override
    public SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionData sourceQuestion,
                                                                           @Nullable SupplementaryStepData latestStep,
                                                                           ViolationData violation,
                                                                           Language lang) {
        return null;
    }

    @Override
    public SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(QuestionData mainQuestion,
                                                                           SupplementaryStepContext step,
                                                                           List<? extends AnswerData> responses,
                                                                           Language language) {
        return null;
    }

    @Override
    public boolean needSupplementaryQuestion(String violatedKnowledgeName, @Nullable InteractionType interactionType) {
        return false;
    }

    @Override
    public String getMessage(String messageKey, Language preferredLanguage) {
        var key = messageKey.startsWith(MESSAGE_PREFIX) ? messageKey : MESSAGE_PREFIX + messageKey;
        var found = localizationService.getMessage(key, Language.getLocale(preferredLanguage));
        return found.equals(key) ? messageKey : found;
    }

    @Override
    protected List<GeneratedQuestionData> getQuestionTemplates() {
        return List.of();
    }
}
