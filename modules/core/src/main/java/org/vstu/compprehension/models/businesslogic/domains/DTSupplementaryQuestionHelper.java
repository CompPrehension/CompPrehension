package org.vstu.compprehension.models.businesslogic.domains;

import its.questions.gen.QuestioningSituation;
import its.questions.gen.formulations.Localization;
import its.questions.gen.formulations.LocalizedDomainModel;
import its.questions.gen.states.Question;
import its.questions.gen.states.*;
import its.questions.gen.strategies.FullBranchStrategy;
import its.questions.gen.strategies.QuestionAutomata;
import lombok.val;
import org.apache.jena.rdf.model.Model;
import org.vstu.compprehension.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.feedback.FeedbackViolationLawDto;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.SingleChoiceOptionsEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DTSupplementaryQuestionHelper {
    public DTSupplementaryQuestionHelper(Domain domain, String domainModelDirectory, Function<InteractionEntity, Model> mainQuestionToModelTransformer) {
        this.domain = domain;
        this.domainModel = new LocalizedDomainModel(domainModelDirectory);
        this.supplementaryAutomata = FullBranchStrategy.INSTANCE.buildAndFinalize(domainModel.decisionTree.getMain(), new EndQuestionState());
        this.mainQuestionToModelTransformer = mainQuestionToModelTransformer;
    }

    private final Domain domain;
    final LocalizedDomainModel domainModel ;
    private final QuestionAutomata supplementaryAutomata;
    private final Function<InteractionEntity, Model> mainQuestionToModelTransformer;

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
        Model m = mainQuestionToModelTransformer.apply(lastInteraction);
        m.add(domainModel.domainRDF);

        //создать ситуацию, описывающую контекст задания вспомогательных вопросов
        QuestioningSituation situation;
        String localizationCode = domainModel.getLocalizations().containsKey(userLang.toLocaleString()) ? userLang.toLocaleString() : domainModel.getLocalizations().keySet().stream().findFirst().orElseThrow();
        if(latestStep != null){
            latestStep.getSituationInfo().setLocalizationCode(localizationCode);
            situation = latestStep.getSituationInfo().toQuestioningSituation(m);
        }
        else {
            situation = new QuestioningSituation(m, localizationCode);
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

        SupplementaryResponse response = stateResultAsSupplementaryResponse(res, mainQuestion.getExerciseAttempt());
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
            answers = new ArrayList(Collections.nCopies(aggregationPadding, 0));
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
        Model m = mainQuestionToModelTransformer.apply(mainQuestionInteraction);
        m.add(domainModel.domainRDF);

        //создать ситуацию, описывающую контекст задания вспомогательных вопросов
        QuestioningSituation situation = supplementaryInfo.getSituationInfo().toQuestioningSituation(m);

        //получить фидбек ответа и изменение состояния
        QuestionStateChange change = state.proceedWithAnswer(situation, answers);

        SupplementaryStepEntity newSupplementaryChain = new SupplementaryStepEntity(
                supplementaryInfo.getMainQuestionInteraction(), situation, null, change.getNextState() != null ? change.getNextState().getId() : null
        );
        return new SupplementaryFeedbackGenerationResult(stateChangeAsSupplementaryFeedbackDto(change), newSupplementaryChain);
    }

    private static final int aggregationPadding = 5; //FIXME используется потому, что из вопросов-сопоставлений можно отправить неполный ответ
    private static final int aggregationShift = 2;
    private org.vstu.compprehension.models.businesslogic.Question transformQuestionFormats(Question q, ExerciseAttemptEntity exerciseAttempt){
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
            val aggregationMatching = AggregationQuestionState.aggregationMatching(Localization.getLocalizations().get(exerciseAttempt.getUser().getPreferred_language().toLocaleString()));
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
            return new Matching(generated, domain);
        }
        else {
            generated.setQuestionType(QuestionType.SINGLE_CHOICE);
            val opt = new SingleChoiceOptionsEntity();
            opt.setShowSupplementaryQuestions(true);
            opt.setDisplayMode(SingleChoiceOptionsEntity.DisplayMode.RADIO);
            generated.setOptions(opt);
            return new SingleChoice(generated, domain);
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
    private SupplementaryResponse stateResultAsSupplementaryResponse(QuestionStateResult q, ExerciseAttemptEntity exerciseAttempt){
        if(q instanceof Question){
            org.vstu.compprehension.models.businesslogic.Question supQuestion = transformQuestionFormats((Question) q, exerciseAttempt);
            return new SupplementaryResponse(supQuestion);
        }
        else {
            QuestionStateChange change = ((QuestionStateChange) q);
            return new SupplementaryResponse(stateChangeAsSupplementaryFeedbackDto(change));
        }
    }

}
