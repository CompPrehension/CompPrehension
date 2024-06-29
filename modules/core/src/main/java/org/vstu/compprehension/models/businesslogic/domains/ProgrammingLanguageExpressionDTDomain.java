package org.vstu.compprehension.models.businesslogic.domains;

import its.model.DomainSolvingModel;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
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
    
    private static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "programming-language-expression-domain-model/";
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
        return Collections.singletonList(new DTLaw(this.domainSolvingModel.getDecisionTree()));
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
        return Collections.singletonList(new DTLaw(this.domainSolvingModel.getDecisionTree()));
    }

    //-----------ФАКТЫ---------------

    @Override
    public Collection<Fact> processQuestionFactsForBackendSolve(Collection<Fact> questionFacts) {
        its.model.definition.Domain situationModel = ProgrammingLanguageExpressionRDFTransformer.questionToDomainModel(
            domainSolvingModel.getDomain(),
            questionFacts.stream().map(Fact::asBackendFact).toList(),
            Collections.emptyList()
        );

        return Collections.singletonList(new DecisionTreeReasonerBackend.DomainFact(situationModel));
    }

    @Override
    public Collection<Fact> processQuestionFactsForBackendJudge(
            Collection<Fact> questionFacts,
            Collection<ResponseEntity> responses,
            Collection<Fact> responseFacts, Collection<Fact> solutionFacts) {
        its.model.definition.Domain situationModel = ProgrammingLanguageExpressionRDFTransformer.questionToDomainModel(
            domainSolvingModel.getDomain(),
            questionFacts.stream().map(Fact::asBackendFact).toList(),
            responses.stream().toList()
        );

        return Collections.singletonList(new DecisionTreeReasonerBackend.DomainFact(situationModel));
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

    @Override
    @Deprecated
    public ProcessSolutionResult processSolution(Collection<Fact> solution) {
        return null;
    }
    
    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        val lastCorrectInteraction = Optional.ofNullable(q.getQuestionData().getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0)
                .reduce((first, second) -> second);
        
        val solution = Fact.entitiesToFacts(q.getSolutionFacts());
        assert solution != null;
        //TODO
        return null;
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
        return true;
    }

    private its.model.definition.Domain mainQuestionToModel(InteractionEntity lastMainQuestionInteraction) {
        return ProgrammingLanguageExpressionRDFTransformer.questionToDomainModel(
            dtSupplementaryQuestionHelper.domainModel.getDomain(),
            lastMainQuestionInteraction.getQuestion(),
            lastMainQuestionInteraction
        );
    }
    
    private final DecisionTreeSupQuestionHelper dtSupplementaryQuestionHelper = new DecisionTreeSupQuestionHelper(
            this,
            this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION),
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
        List<ViolationEntity> mistakes = DecisionTreeReasonerBackend.reasonerOutputFactsToViolations(
            violations.stream().toList()
        );
        
        InterpretSentenceResult result = new InterpretSentenceResult();
        result.violations = mistakes;
        result.correctlyAppliedLaws = new ArrayList<>();
        result.isAnswerCorrect = mistakes.isEmpty();

//        ProcessSolutionResult processResult = processSolution(violations);
        result.CountCorrectOptions = 10;
        result.IterationsLeft = 10; //TODO
        return result;
    }

    @Override
    public List<HyperText> makeExplanations(List<Fact> reasonerOutputFacts, Language lang) {
        return DecisionTreeReasonerBackend.makeExplanations(reasonerOutputFacts, lang);
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
