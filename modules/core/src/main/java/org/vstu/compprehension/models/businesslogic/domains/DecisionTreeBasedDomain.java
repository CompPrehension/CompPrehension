package org.vstu.compprehension.models.businesslogic.domains;

import its.questions.gen.QuestioningSituation;
import its.questions.gen.formulations.LocalizedDomainModel;
import its.questions.gen.states.*;
import its.questions.gen.strategies.FullBranchStrategy;
import its.questions.gen.strategies.QuestionAutomata;
import org.apache.jena.rdf.model.Model;
import org.springframework.data.util.Pair;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.vstu.compprehension.Service.QuestionService.aggregationPadding;
import static org.vstu.compprehension.Service.QuestionService.aggregationShift;

public abstract class DecisionTreeBasedDomain extends Domain {
    public DecisionTreeBasedDomain(DomainEntity domainEntity, RandomProvider randomProvider) {
        super(domainEntity, randomProvider);
    }

    protected abstract String getDomainModelDirectory();

    protected final LocalizedDomainModel domainModel = new LocalizedDomainModel(getDomainModelDirectory());
    private final QuestionAutomata supplementaryAutomata = FullBranchStrategy.INSTANCE.buildAndFinalize(domainModel.decisionTree.getMain(), new EndQuestionState());

    protected abstract Model mainQuestionToModel(InteractionEntity lastMainQuestionInteraction);

    //DT = Decision Tree
    public Pair<QuestionStateResult, SupplementaryStepEntity> makeSupplementaryQuestionDT(QuestionEntity mainQuestion, Language userLang) {
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
        Model m = mainQuestionToModel(lastInteraction);

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
        return Pair.of(res, supplementaryChain);
    }

    public Pair<QuestionStateChange, SupplementaryStepEntity> judgeSupplementaryQuestionDT(SupplementaryStepEntity supplementaryInfo, List<ResponseEntity> responses){
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
        Model m = mainQuestionToModel(mainQuestionInteraction);

        //создать ситуацию, описывающую контекст задания вспомогательных вопросов
        QuestioningSituation situation = supplementaryInfo.getSituationInfo().toQuestioningSituation(m);

        //получить фидбек ответа и изменение состояния
        QuestionStateChange change = state.proceedWithAnswer(situation, answers);

        SupplementaryStepEntity newSupplementaryChain = new SupplementaryStepEntity(
                supplementaryInfo.getMainQuestionInteraction(), situation, null, change.getNextState() != null ? change.getNextState().getId() : null
        );
        return Pair.of(change, newSupplementaryChain);
    }
}
