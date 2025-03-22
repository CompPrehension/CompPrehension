package org.vstu.compprehension.models.businesslogic.domains;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.questions.gen.QuestioningSituation;
import its.questions.gen.formulations.Localization;
import its.questions.gen.states.*;
import its.questions.gen.strategies.FullBranchStrategy;
import its.questions.gen.strategies.QuestionAutomata;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.feedback.FeedbackViolationLawDto;
import org.vstu.compprehension.models.businesslogic.SupplementaryFeedbackGenerationResult;
import org.vstu.compprehension.models.businesslogic.SupplementaryResponse;
import org.vstu.compprehension.models.businesslogic.SupplementaryResponseGenerationResult;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.SingleChoiceOptionsEntity;

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
        Function<InteractionEntity, DomainModel> mainQuestionToModelTransformer
    ) {
        this.domain = domain;
        this.domainModel = domainSolvingModel;
        this.supplementaryAutomata = FullBranchStrategy.INSTANCE.buildAndFinalize(
            domainModel.getDecisionTree().getMainBranch(), new EndQuestionState()
        );
        this.mainQuestionToModelTransformer = mainQuestionToModelTransformer;
    }

    public DecisionTreeSupQuestionHelper(
        Domain domain,
        URL domainModelDirectoryURL,
        Function<InteractionEntity, DomainModel> mainQuestionToModelTransformer
    ) {
        this(
            domain,
            new DomainSolvingModel(domainModelDirectoryURL, DomainSolvingModel.BuildMethod.LOQI),
            mainQuestionToModelTransformer
        );
    }

    private final Domain domain;
    final DomainSolvingModel domainModel ;
    private final QuestionAutomata supplementaryAutomata;
    private final Function<InteractionEntity, DomainModel> mainQuestionToModelTransformer;

    //DT = Decision Tree
    protected SupplementaryResponseGenerationResult makeSupplementaryQuestion(QuestionEntity mainQuestion, Language userLang) {
        //Получить ошибочную интеракцию с основным вопросом
        List<InteractionEntity> interactions = mainQuestion.getInteractions();
        if (interactions == null || interactions.isEmpty()) {
            return null;
        }
        InteractionEntity lastInteraction = interactions.get(interactions.size() - 1);
        //Получить последний шаг цепочки вспомогательных вопросов
        List<SupplementaryStepEntity> supplementarySteps = lastInteraction.getRelatedSupplementarySteps();
        SupplementaryStepEntity latestStep = supplementarySteps.isEmpty() ? null : supplementarySteps.get(supplementarySteps.size() - 1);

        //Создать соответствующую ситуации рдф-модель
        DomainModel situationModel = mainQuestionToModelTransformer.apply(lastInteraction);

        //создать ситуацию, описывающую контекст задания вспомогательных вопросов
        QuestioningSituation situation;
        String localizationCode = userLang.toLocaleString(); //FIXME должна быть какая-то проверка на то, какие языки поддерживает модель
        if(latestStep != null){
            latestStep.getSituationInfo().setLocalizationCode(localizationCode);
            situation = latestStep.getSituationInfo().toQuestioningSituation(situationModel);
        }
        else {
            situation = new QuestioningSituation(situationModel, localizationCode);
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

        SupplementaryStepEntity supplementaryChain = new SupplementaryStepEntity(lastInteraction, situation, null,
                res instanceof  QuestionStateChange
                        ? ((QuestionStateChange) res).getNextState() != null ? ((QuestionStateChange) res).getNextState().getId() : 0
                        : state.getId()
        );

        SupplementaryResponse response = stateResultAsSupplementaryResponse(res, mainQuestion.getExerciseAttempt(), userLang);
        if(response.getQuestion() != null){
            supplementaryChain.setSupplementaryQuestion(response.getQuestion().getQuestionData());
        }
        return new SupplementaryResponseGenerationResult(response, supplementaryChain);
    }

    protected SupplementaryFeedbackGenerationResult judgeSupplementaryQuestion(SupplementaryStepEntity supplementaryInfo, List<ResponseEntity> responses){
        //получить состояние автомата вопросов, соответствующее данному вопросу
        QuestionState state = supplementaryAutomata.get(supplementaryInfo.getNextStateId());
        //преобразовать ответы
        List<Integer> answers = null;
        if(state instanceof AggregationQuestionState || state instanceof RedirectQuestionState && ((RedirectQuestionState) state).redirectsTo() instanceof AggregationQuestionState){
            answers = new ArrayList<>(Collections.nCopies(aggregationPadding, 0));
            for(ResponseEntity r : responses){
                answers.set(r.getLeftAnswerObject().getAnswerId()-aggregationShift, r.getRightAnswerObject().getAnswerId());
            }
        }
        else {
            assert responses.size() == 1;
            answers = List.of(responses.get(0).getLeftAnswerObject().getAnswerId());
        }

        //Создать соответствующую ситуации рдф-модель
        InteractionEntity mainQuestionInteraction = supplementaryInfo.getMainQuestionInteraction();
        DomainModel situationModel = mainQuestionToModelTransformer.apply(mainQuestionInteraction);

        //создать ситуацию, описывающую контекст задания вспомогательных вопросов
        QuestioningSituation situation = supplementaryInfo.getSituationInfo().toQuestioningSituation(situationModel);

        //получить фидбек ответа и изменение состояния
        QuestionStateChange change = state.proceedWithAnswer(situation, answers);

        SupplementaryStepEntity newSupplementaryChain = new SupplementaryStepEntity(
                supplementaryInfo.getMainQuestionInteraction(), situation, null, change.getNextState() != null ? change.getNextState().getId() : null
        );
        return new SupplementaryFeedbackGenerationResult(stateChangeAsSupplementaryFeedbackDto(change), newSupplementaryChain);
    }

    private static final int aggregationPadding = 5; //FIXME используется потому, что из вопросов-сопоставлений можно отправить неполный ответ
    private static final int aggregationShift = 2;
    private org.vstu.compprehension.models.businesslogic.Question transformQuestionFormats(Question q, @Nullable ExerciseAttemptEntity exerciseAttempt, Language language){
        QuestionEntity generated = new QuestionEntity();
        generated.setQuestionText(q.getText());
        //generated.setQuestionName(String.valueOf(creatorStateId));    //FIXME?
        generated.setQuestionDomainType(domain.getDefaultQuestionType(true));
        generated.setExerciseAttempt(exerciseAttempt);
        generated.setAnswerObjects(q.getOptions().stream().map((opt) -> {
            AnswerObjectEntity ans = new AnswerObjectEntity();
            ans.setAnswerId(opt.getSecond());
            ans.setHyperText(opt.getFirst());
            return ans;
        }).collect(Collectors.toList()));
        if(q.isAggregation() ){
            generated.setQuestionType(QuestionType.MATCHING);
            List<AnswerObjectEntity> answers = generated.getAnswerObjects();
            for(AnswerObjectEntity a : answers){
                a.setAnswerId(a.getAnswerId() + aggregationShift); //чтобы избежать пересечения с answerId ответов в aggregationMathching
            }
            val aggregationMatching = AggregationQuestionState.aggregationMatching(Localization.getLocalizations().get(language.toLocaleString()));
            for(Map.Entry<String, Integer> m : aggregationMatching.entrySet()){
                AnswerObjectEntity ans = new AnswerObjectEntity();
                ans.setAnswerId(m.getValue());
                ans.setHyperText(m.getKey());
                ans.setRightCol(true);
                answers.add(ans);
            }
            generated.setAnswerObjects(answers);
            val opt = new MatchingQuestionOptionsEntity();
            opt.setShowSupplementaryQuestions(true);
            opt.setDisplayMode(MatchingQuestionOptionsEntity.DisplayMode.COMBOBOX);
            generated.setOptions(opt);
            return new org.vstu.compprehension.models.businesslogic.Question(generated, domain);
        }
        else {
            generated.setQuestionType(QuestionType.SINGLE_CHOICE);
            val opt = new SingleChoiceOptionsEntity();
            opt.setShowSupplementaryQuestions(true);
            opt.setDisplayMode(SingleChoiceOptionsEntity.DisplayMode.RADIO);
            generated.setOptions(opt);
            return new org.vstu.compprehension.models.businesslogic.Question(generated, domain);
        }
    }
    private static SupplementaryFeedbackDto stateChangeAsSupplementaryFeedbackDto(QuestionStateChange change){
        Explanation expl = change.getExplanation();
        return new SupplementaryFeedbackDto(
                new FeedbackDto.Message(expl != null && expl.getType() == ExplanationType.Error ? FeedbackDto.MessageType.ERROR : FeedbackDto.MessageType.SUCCESS, expl != null ? expl.getText() : "...", new FeedbackViolationLawDto("", true)),
                change.getNextState() == null ||
                        change.getNextState() instanceof EndQuestionState ||
                        (change.getNextState() instanceof RedirectQuestionState && ((RedirectQuestionState) change.getNextState()).redirectsTo() instanceof EndQuestionState)
                        ? SupplementaryFeedbackDto.Action.Finish
                        : expl != null && expl.getShouldPause() ? SupplementaryFeedbackDto.Action.ContinueManual : SupplementaryFeedbackDto.Action.ContinueAuto
        );
    }
    private SupplementaryResponse stateResultAsSupplementaryResponse(QuestionStateResult q, @Nullable ExerciseAttemptEntity exerciseAttempt, Language language){
        if(q instanceof Question){
            org.vstu.compprehension.models.businesslogic.Question supQuestion = transformQuestionFormats((Question) q, exerciseAttempt, language);
            return new SupplementaryResponse(supQuestion);
        }
        else {
            QuestionStateChange change = ((QuestionStateChange) q);
            return new SupplementaryResponse(stateChangeAsSupplementaryFeedbackDto(change));
        }
    }

}
