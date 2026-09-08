package org.vstu.compprehension.businesslogic.domains;

import its.model.definition.ObjectRef;
import its.reasoner.LearningSituation;
import org.vstu.compprehension.enums.SupplementaryBranchResult;
import org.vstu.compprehension.services.SupplementaryStepDataService;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.data.question.NewSupplementaryStepData;
import org.vstu.compprehension.data.question.SupplementarySituationData;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.data.questionoptions.MatchingQuestionOptionsData;
import org.vstu.compprehension.data.questionoptions.MultiChoiceOptionsData;
import org.vstu.compprehension.data.questionoptions.SingleChoiceOptionsData;
import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.nodes.BranchResult;
import its.questions.gen.QuestioningSituation;
import its.questions.gen.states.*;
import its.questions.gen.strategies.FullBranchStrategy;
import its.questions.gen.strategies.QuestionAutomata;
import kotlin.Pair;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackViolationLawDto;
import org.vstu.compprehension.businesslogic.SupplementaryFeedbackGenerationResult;
import org.vstu.compprehension.businesslogic.SupplementaryResponse;
import org.vstu.compprehension.businesslogic.SupplementaryResponseGenerationResult;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DecisionTreeSupQuestionHelper {
    public DecisionTreeSupQuestionHelper(
            Domain domain,
            DomainSolvingModel domainSolvingModel,
            Function<QuestionInteractionData, DomainModel> mainQuestionToModelTransformer,
            SupplementaryStepDataService supplementaryStepService
    ) {
        this.domain = domain;
        this.domainModel = domainSolvingModel;
        this.supplementaryStepService = supplementaryStepService;
        this.supplementaryAutomata = FullBranchStrategy.INSTANCE.buildAndFinalize(
                domainModel.getDecisionTree().getMainBranch(), new EndQuestionState()
        );
        this.mainQuestionToModelTransformer = mainQuestionToModelTransformer;
    }

    public DecisionTreeSupQuestionHelper(
            DomainBase domain,
            URL domainModelDirectoryURL,
            Function<QuestionInteractionData, DomainModel> mainQuestionToModelTransformer,
            SupplementaryStepDataService supplementaryStepService
    ) {
        this(
                domain,
                new DomainSolvingModel(domainModelDirectoryURL, DomainSolvingModel.BuildMethod.LOQI),
                mainQuestionToModelTransformer,
                supplementaryStepService
        );
    }

    private final Domain domain;
    final DomainSolvingModel domainModel ;
    private final QuestionAutomata supplementaryAutomata;
    private final Function<QuestionInteractionData, DomainModel> mainQuestionToModelTransformer;
    private final SupplementaryStepDataService supplementaryStepService;

    //DT = Decision Tree
    public SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionData mainQuestion, Language userLang) {
        //Получить ошибочную интеракцию с основным вопросом
        List<QuestionInteractionData> interactions = mainQuestion.getInteractions();
        if (interactions == null || interactions.isEmpty()) {
            return null;
        }
        QuestionInteractionData lastInteraction = interactions.get(interactions.size() - 1);
        //Получить последний шаг цепочки вспомогательных вопросов
        // Шаги цепочки — записи в БД, у взаимодействия в бизнес-логике их нет:
        // спрашиваем сервис по идентификатору.
        SupplementaryStepData latestStep = supplementaryStepService.findLatestStepOfInteraction(lastInteraction.getId());

        //Создать соответствующую ситуации рдф-модель
        DomainModel situationModel = mainQuestionToModelTransformer.apply(lastInteraction);

        //создать ситуацию, описывающую контекст задания вспомогательных вопросов
        QuestioningSituation situation;
        String localizationCode = userLang.toLocaleString(); //FIXME должна быть какая-то проверка на то, какие языки поддерживает модель
        if(latestStep != null){
            latestStep.getSituationInfo().setLocalizationCode(localizationCode);
            situation = toQuestioningSituation(latestStep.getSituationInfo(), situationModel);
        }
        else {
            situation = new QuestioningSituation(situationModel, localizationCode);
            situation.addAssumedResult(domainModel.getDecisionTree().getMainBranch(), BranchResult.CORRECT);
        }

        //получить состояние автомата вопросов, к которому перешли на последнем шаге
        QuestionState state = latestStep != null ? supplementaryAutomata.get(latestStep.getNextStateId()) : supplementaryAutomata.getInitState();


        //Получить вопрос
        QuestionStateResult res = state.getQuestion(situation);
        while(res instanceof QuestionStateChange &&
                ((QuestionStateChange) res).getExplanation() == null &&
                ((QuestionStateChange) res).getNextState() != null && !(((QuestionStateChange) res).getNextState() instanceof EndQuestionState)){
            state = ((QuestionStateChange) res).getNextState();
            res = state.getQuestion(situation);
        }

        NewSupplementaryStepData supplementaryChain = new NewSupplementaryStepData(
                lastInteraction.getId(),
                toSupplementarySituationData(situation),
                res instanceof  QuestionStateChange
                        ? ((QuestionStateChange) res).getNextState() != null ? ((QuestionStateChange) res).getNextState().getId() : 0
                        : state.getId()
        );

        // Попытка и связь шага со сгенерированным вопросом проставляются сервисом
        // после сохранения: у домена нет ни сущности попытки, ни сущности вопроса.
        SupplementaryResponse response = stateResultAsSupplementaryResponse(res, null, userLang);
        return new SupplementaryResponseGenerationResult(response, supplementaryChain);
    }

    public SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(SupplementaryStepData supplementaryInfo, List<ResponseData> responses){
        //получить состояние автомата вопросов, соответствующее данному вопросу
        QuestionState state = supplementaryAutomata.get(supplementaryInfo.getNextStateId());

        QuestionInteractionData mainQuestionInteraction = supplementaryStepService.getMainQuestionInteraction(supplementaryInfo.getMainQuestionInteractionId());
        DomainModel situationModel = mainQuestionToModelTransformer.apply(mainQuestionInteraction);

        //создать ситуацию, описывающую контекст задания вспомогательных вопросов
        QuestioningSituation situation = toQuestioningSituation(supplementaryInfo.getSituationInfo(), situationModel);

        //преобразовать ответы
        List<Integer> answers = null;
        if (state.getQuestion(situation) instanceof Question question) {
            switch (question.getType()) {
                case single -> {
                    assert responses.size() == 1;
                    answers = List.of(responses.get(0).getLeftAnswerObject().getAnswerId());
                }
                case multiple -> {
                    answers = responses.stream()
                        .map(ResponseData::getLeftAnswerObject)
                        .map(AnswerObjectData::getAnswerId)
                        .collect(Collectors.toList());
                }
                case matching -> {
                    answers = new ArrayList<>(Collections.nCopies(question.getOptions().size(), 0));
                    for (ResponseData r : responses) {
                        answers.set(
                            r.getLeftAnswerObject().getAnswerId(),
                            r.getRightAnswerObject().getAnswerId() - question.getOptions().size()
                        );
                    }
                }
            }
        }

        //получить фидбек ответа и изменение состояния
        QuestionStateChange change = state.proceedWithAnswer(situation, answers);

        NewSupplementaryStepData newSupplementaryChain = new NewSupplementaryStepData(
                supplementaryInfo.getMainQuestionInteractionId(),
                toSupplementarySituationData(situation),
                change.getNextState() != null ? change.getNextState().getId() : null
        );
        return new SupplementaryFeedbackGenerationResult(stateChangeAsSupplementaryFeedbackDto(change), newSupplementaryChain);
    }

    private org.vstu.compprehension.businesslogic.Question transformQuestionFormats(Question q, @Nullable ExerciseOptionsData exerciseOptions, Language language){
        QuestionData generated = new QuestionData();
        generated.setQuestionText(q.getText());
        //generated.setQuestionName(String.valueOf(creatorStateId));    //FIXME?
        generated.setQuestionDomainType(domain.getDefaultQuestionType(true));
        generated.setAnswerObjects(
            q.getOptions().stream()
                .map(opt -> {
                    AnswerObjectData ans = new AnswerObjectData();
                    ans.setAnswerId(opt.getSecond());
                    ans.setHyperText(opt.getFirst());
                    return ans;
                })
                .collect(Collectors.toList())
        );
        switch (q.getType()) {
            case matching -> {
                List<AnswerObjectData> answers = generated.getAnswerObjects();
                int matchOptionsShift = answers.size(); //чтобы избежать пересечения с answerId ответов
                for (Pair<String, Integer> m : q.getMatchingOptions()) {
                    AnswerObjectData ans = new AnswerObjectData();
                    ans.setAnswerId(m.getSecond() + matchOptionsShift);
                    ans.setHyperText(m.getFirst());
                    ans.setRightCol(true);
                    answers.add(ans);
                }
                generated.setAnswerObjects(answers);

                generated.setQuestionType(QuestionType.MATCHING);
                val opt = new MatchingQuestionOptionsData();
                opt.setDisplayMode(MatchingQuestionOptionsData.DisplayMode.COMBOBOX);
                generated.setOptions(opt);
            }
            case single -> {
                generated.setQuestionType(QuestionType.SINGLE_CHOICE);
                val opt = new SingleChoiceOptionsData();
                opt.setDisplayMode(SingleChoiceOptionsData.DisplayMode.RADIO);
                generated.setOptions(opt);
            }
            case multiple -> {
                generated.setQuestionType(QuestionType.MULTI_CHOICE);
                val opt = new MultiChoiceOptionsData();
                opt.setDisplayMode(MultiChoiceOptionsData.DisplayMode.SWITCH);
                generated.setOptions(opt);
            }
        }
        generated.getOptions().setShowSupplementaryQuestions(true);
        return new org.vstu.compprehension.businesslogic.Question(generated, domain);
    }

    private static SupplementaryFeedbackDto stateChangeAsSupplementaryFeedbackDto(QuestionStateChange change){
        Explanation expl = change.getExplanation();
        return new SupplementaryFeedbackDto(
                new FeedbackDto.Message(expl != null && expl.getType() == ExplanationType.Error ? FeedbackDto.MessageType.ERROR : FeedbackDto.MessageType.SUCCESS, expl != null ? expl.getText() : "...", List.of(
                        new FeedbackViolationLawDto("", true))),
                change.getNextState() == null ||
                        change.getNextState() instanceof EndQuestionState ||
                        (change.getNextState() instanceof RedirectQuestionState && ((RedirectQuestionState) change.getNextState()).redirectsTo() instanceof EndQuestionState)
                        ? SupplementaryFeedbackDto.Action.Finish
                        : expl != null && expl.getShouldPause() ? SupplementaryFeedbackDto.Action.ContinueManual : SupplementaryFeedbackDto.Action.ContinueAuto
        );
    }
    private SupplementaryResponse stateResultAsSupplementaryResponse(QuestionStateResult q, @Nullable ExerciseOptionsData exerciseOptions, Language language){
        if(q instanceof Question){
            org.vstu.compprehension.businesslogic.Question supQuestion = transformQuestionFormats((Question) q, exerciseOptions, language);
            return new SupplementaryResponse(supQuestion);
        }
        else {
            QuestionStateChange change = ((QuestionStateChange) q);
            return new SupplementaryResponse(stateChangeAsSupplementaryFeedbackDto(change));
        }
    }

    private SupplementarySituationData toSupplementarySituationData(QuestioningSituation situation) {
        var reasoningVariables = situation.getDecisionTreeVariables()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getObjectName()));
        var discussedVariables = situation.getDiscussedVariables();
        var givenAnswers = situation.getGivenAnswers();
        var assumedResults = situation.getAssumedResults()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> fromBranchResult(e.getValue())));
        var localizationCode = situation.getLocalizationCode();
        
        return new SupplementarySituationData(
                reasoningVariables,
                discussedVariables,
                givenAnswers,
                assumedResults,
                localizationCode
        );
    }

    private QuestioningSituation toQuestioningSituation(SupplementarySituationData situation, DomainModel situationModel) {
        Map<String, ObjectRef> vars = situation
                .getReasoningVariables()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new ObjectRef(e.getValue())));
        vars.putAll(LearningSituation.collectDecisionTreeVariables(situationModel));

        var assumedResults = situation.getAssumedResults()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> toBranchResult(e.getValue())));
        
        return new QuestioningSituation(situationModel, vars, situation.getDiscussedVariables(), situation.getGivenAnswers(), assumedResults, situation.getLocalizationCode());
    }

    private SupplementaryBranchResult fromBranchResult(BranchResult branchResult) {
        return switch(branchResult) {
            case BranchResult.CORRECT -> SupplementaryBranchResult.CORRECT;
            case BranchResult.ERROR -> SupplementaryBranchResult.ERROR;
            case BranchResult.NULL ->  SupplementaryBranchResult.NULL;
            default -> throw new IllegalStateException("Unsupported value: " + branchResult);
        };
    }

    private BranchResult toBranchResult(SupplementaryBranchResult supBranchResult) {
        return switch(supBranchResult) {
            case SupplementaryBranchResult.CORRECT -> BranchResult.CORRECT;
            case SupplementaryBranchResult.ERROR -> BranchResult.ERROR;
            case SupplementaryBranchResult.NULL ->  BranchResult.NULL;
            default -> throw new IllegalStateException("Unsupported value: " + supBranchResult);
        };
    }
}
