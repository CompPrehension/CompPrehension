package org.vstu.compprehension.models.businesslogic.domains;

import its.model.DomainSolvingModel;
import its.model.definition.EnumValueRef;
import its.model.definition.ObjectDef;
import its.reasoner.LearningSituation;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.domains.helpers.DecisionTreeHelper;
import org.vstu.compprehension.models.businesslogic.domains.helpers.ProgrammingLanguageExpressionRDFTransformer;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.*;

@Log4j2
public class ProgrammingLanguageExpressionDTDomain extends ProgrammingLanguageExpressionDomain {
    public static final String MESSAGES_CONFIG_PATH = "classpath:/" + RESOURCES_LOCATION + "programming-language-expression-domain-dt-messages";
    static final String MESSAGE_PREFIX = "expr_domain_dt.";

    @Override
    public String getShortnameForQuestionSearch() {
        return "expression"; //
    }
    
    @Override
    public String getSolvingBackendId() {
        return DecisionTreeReasonerBackend.BACKEND_ID;
    }
    
    @SneakyThrows
    public ProgrammingLanguageExpressionDTDomain(
            DomainEntity domainEntity,
            LocalizationService localizationService,
            RandomProvider randomProvider,
            QuestionBank qMetaStorage) {

        super(domainEntity, localizationService, randomProvider, qMetaStorage);

        //LOOK readSupplementaryConfig(this.getClass().getClassLoader().getResourceAsStream(SUPPLEMENTARY_CONFIG_PATH));
    }
    
    public static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "programming-language-expression-domain-model/";
    private final DomainSolvingModel domainSolvingModel = new DomainSolvingModel(
            this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION), //FIXME
            DomainSolvingModel.BuildMethod.LOQI
    );

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

    // filter positive laws by question type and tags
    @Override
    public List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE) || questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE) || questionDomainType.equals(OPERANDS_TYPE_QUESTION_TYPE)
            || questionDomainType.equals(PRECEDENCE_TYPE_QUESTION_TYPE)) {
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

    @Override
    public Set<DomainToBackendInterface<?, ?, ?>> createBackendInterfaces() {
        return Set.of(
            new DecisionTreeReasonerBackend.Interface() {

                @Override
                public DecisionTreeReasonerBackend.Input prepareBackendInfoForJudge(
                    Question question,
                    List<ResponseEntity> responses,
                    List<Tag> tags
                ) {
                    return new DecisionTreeReasonerBackend.Input(
                        ProgrammingLanguageExpressionRDFTransformer.questionToDomainModel(
                            domainSolvingModel.getMergedTagDomain("c++"),
                            domainSolvingModel.getDecisionTrees(),
                            question,
                            responses
                        ),
                        domainSolvingModel.getDecisionTree()
                    );
                }

                @Override
                protected InterpretSentenceResult interpretJudgeNotPerformed(
                    Question judgedQuestion,
                    LearningSituation preparedSituation
                ) {
                    ViolationEntity violation = new ViolationEntity();
                    violation.setLawName(STILL_UNEVALUATED_LEFT_VIOLATION_NAME);
                    violation.setViolationFacts(new ArrayList<>());
                    InterpretSentenceResult result = new InterpretSentenceResult();
                    result.violations = List.of(violation);
                    result.explanations = List.of(new HyperText(
                        locCodeToStillUnevaluatedElementsLeftFormulationsMap().get(
                            getUserLanguageByQuestion(judgedQuestion).toLocaleString().toUpperCase()
                        )
                    ));
                    updateInterpretationResult(result, preparedSituation);
                    return result;
                }

                private Map<String, String> locCodeToStillUnevaluatedElementsLeftFormulationsMap(){
                    return Map.ofEntries(
                        Pair.of("RU", "В выражении все еще есть невычисленные операторы"),
                        Pair.of("EN", "There are still unevaluated operators left in the expression")
                    );
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
                ){
                    interpretationResult.CountCorrectOptions = 1; //TODO? Непонятно зачем оно надо
                    interpretationResult.IterationsLeft = (int) situation.getDomain().getObjects()
                        .stream().filter(objectDef ->
                            hasState(objectDef, "unevaluated")
                                || (hasState(objectDef, "omitted")
                                    && getParent(objectDef).map(parent -> hasState(parent, "unevaluated")).orElse(false))
                        )
                        .count();
                }

                private Optional<ObjectDef> getParent(ObjectDef object){
                    return object.getRelationshipLinks().listByName("isOperandOf").stream().findFirst()
                        .map(link -> link.getObjects().get(0));
                }

                private boolean hasState(ObjectDef object, String stateValueName){
                    if(!object.isInstanceOf("operand")){
                        return false;
                    }
                    return new EnumValueRef("state", stateValueName).equals(object.getPropertyValue("state"));
                }

                @Override
                public DecisionTreeReasonerBackend.Input prepareBackendInfoForSolve(Question question, List<Tag> tags) {
                    return null; //Solve not used in DecisionTreeReasonerBackend
                }
            }
        );
    }

    @Override
    public Collection<Fact> responseToFacts(
        String questionDomainType,
        List<ResponseEntity> responses,
        List<AnswerObjectEntity> answerObjects
    ) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
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
                            END_EVALUATION,
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
        } else if (questionDomainType.equals(DEFINE_TYPE_QUESTION_TYPE)) {
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
        } else if (questionDomainType.equals(OPERANDS_TYPE_QUESTION_TYPE)) {
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
        } else if (questionDomainType.equals(PRECEDENCE_TYPE_QUESTION_TYPE)) {
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
    @Deprecated
    public ProcessSolutionResult processSolution(Collection<Fact> solution) {
        return null;
    }

    @Override
    public List<HyperText> getFullSolutionTrace(Question question) {
        //TODO - this probably needs to be moved inside a BackendInterface and redone
        return super.getFullSolutionTrace(question);
    }

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        //TODO - this probably needs to be moved inside a BackendInterface and redone
        return super.getAnyNextCorrectAnswer(q);
    }
    
    @Override
    @Deprecated
    public Set<String> possibleViolations(Question q, List<ResponseEntity> completedSteps) {
        return null;
    }
    
    @Override
    @Deprecated
    public Set<Set<String>> possibleViolationsByStep(Question q, List<ResponseEntity> completedSteps) {
        return null;
    }

    //----------Вспомогательные вопросы------------
    
    @Override
    public boolean needSupplementaryQuestion(ViolationEntity violation) {
        return !STILL_UNEVALUATED_LEFT_VIOLATION_NAME.equals(violation.getLawName());
    }

    private its.model.definition.Domain mainQuestionToModel(InteractionEntity lastMainQuestionInteraction) {
        return ProgrammingLanguageExpressionRDFTransformer.questionToDomainModel(
            domainSolvingModel.getMergedTagDomain("c++"),
            domainSolvingModel.getDecisionTrees(),
            new Question(lastMainQuestionInteraction.getQuestion(), this),
            lastMainQuestionInteraction.getResponses()
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
    public List<HyperText> makeExplanation(List<ViolationEntity> mistakes, FeedbackType feedbackType, Language lang) {
        ArrayList<HyperText> result = new ArrayList<>();
        for (ViolationEntity mistake : mistakes) {
            result.add(makeSingleExplanation(mistake, feedbackType, lang));
        }
        return result;
    }
    
    private HyperText makeSingleExplanation(ViolationEntity mistake, FeedbackType feedbackType, Language lang) {
        return new HyperText("WRONG");
    }
}
