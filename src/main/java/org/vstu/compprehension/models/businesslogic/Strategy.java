package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.Service.DomainService;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.DisplayingFeedbackType;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import org.vstu.compprehension.utils.DomainAdapter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.lang.Math.abs;

@Component
public class Strategy extends AbstractStrategy {

    @Autowired
    private DomainService domainService;

    public QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt) {

        QuestionRequest qr = new QuestionRequest();
        qr.setExerciseAttempt(exerciseAttempt);
        ExerciseEntity exercise = exerciseAttempt.getExercise();
        Domain domain = DomainAdapter.getDomain(domainService.getDomainEntity(exercise.getDomain().getName()).getName());

        List<Concept> deniedConcepts = new ArrayList<>();
        List<Concept> allowedConcepts = new ArrayList<>();
        List<Concept> targetConcepts = new ArrayList<>();

        //Выделить из упражнения целевые и запрещенные законы
        for (ExerciseConceptEntity ec : exercise.getExerciseConcepts()) {
            if (ec.getRoleInExercise() == RoleInExercise.TARGETED) {
                targetConcepts.add(domain.getConcept(ec.getConceptName()));
            } else if (ec.getRoleInExercise() == RoleInExercise.FORBIDDEN) {
                deniedConcepts.add(domain.getConcept(ec.getConceptName()));
            }
        }

        if(targetConcepts.contains(domain.getConcept("precedence"))){
            allowedConcepts.add(domain.getConcept("operator_binary_*"));
            allowedConcepts.add(domain.getConcept("operator_binary_+"));
        }else{
            allowedConcepts.add(domain.getConcept("operator_binary_+"));
        }

        qr.setComplexity(1);
        qr.setDeniedConcepts(deniedConcepts);
        qr.setSolvingDuration(30);
        qr.setTargetConcepts(targetConcepts);
        qr.setAllowedConcepts(allowedConcepts);

        List<Law> laws = new ArrayList<>(domain.getNegativeLaws()); //domainEntity.getLaws();

        List<Law> targetLaws = new ArrayList<>();
        List<Law> deniedLaws = new ArrayList<>();
        //Распределяем законы не запрещенные и целевые
        for (Law l : laws) {
            boolean isLawContainsDeniedConcepts = false;
            boolean isLawContainsTargetConcepts = false;
            //Проверить, содержит ли закон запрещенные концепты
            for (int i = 0; i < deniedConcepts.size() && isLawContainsDeniedConcepts == false; ++i) {
                isLawContainsDeniedConcepts = l.getConcepts().contains(
                        deniedConcepts.get(i));
            }

            //Проверить, содержит ли закон целевые концепты
            for (int i = 0; i < targetConcepts.size() && isLawContainsTargetConcepts == false; ++i) {
                isLawContainsTargetConcepts = l.getConcepts().contains(
                        targetConcepts.get(i));
            }
            if (isLawContainsDeniedConcepts) {
                deniedLaws.add(l);
            } else if (isLawContainsTargetConcepts) {
                targetLaws.add(l);
            }
        }

        qr.setDeniedLaws(deniedLaws);
        qr.setTargetLaws(targetLaws);

        return qr;
    }

    public DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question) {

        List<InteractionEntity> interactions = question.getInteractions();

        int interactionWithMistakes = 0;
        for (InteractionEntity i : interactions) {

            if (i.getMistakes() != null || i.getMistakes().size() != 0) {

                interactionWithMistakes++;
            }
        }

        if (interactionWithMistakes < 1) {
            return DisplayingFeedbackType.NOT_SHOW;
        } else if (interactionWithMistakes < 2) {
            return DisplayingFeedbackType.HOVER;
        } else {
            return DisplayingFeedbackType.SHOW;
        }
    }

    public FeedbackType determineFeedbackType(QuestionEntity question) {

        List<InteractionEntity> interactions = question.getInteractions();

        int interactionWithMistakes = 0;
        for (InteractionEntity i : interactions) {

            if (i.getMistakes() != null || i.getMistakes().size() != 0) {

                interactionWithMistakes++;
            }
        }

        if (interactionWithMistakes < 1) {
            return FeedbackType.DEGREE_OF_CORRECTNESS;
        } else {
            return FeedbackType.EXPLANATION;
        }
    }

    @Override
    public float grade(ExerciseAttemptEntity exerciseAttempt) {

        val res = getLawGrade(exerciseAttempt);
        if(res.keySet().isEmpty()){
            return (float)0;
        }

        float summary = 0;

        for(HashMap.Entry<String, Float> entry : res.entrySet()) {
            String key = entry.getKey();
            val value = entry.getValue();
            summary += value;
        }

        return summary/(float)res.keySet().stream().count();
    }

    @Override
    public Decision decide(ExerciseAttemptEntity exerciseAttempt) {
        if(exerciseAttempt.getQuestions().stream().count() >= 15){
            return Decision.FINISH;
        }
        return Decision.CONTINUE;
    }

    protected HashMap<String, Float> getLawGrade(ExerciseAttemptEntity exerciseAttempt){
        HashMap<String, Float> res = new HashMap<>();

        HashMap<String, ArrayList<Boolean>> conceptAtempt = new HashMap<>();
        ArrayList<QuestionEntity> questions = new ArrayList<>();
        questions.addAll(exerciseAttempt.getQuestions());

        ArrayList<InteractionEntity> ies = new ArrayList<>();
        for(QuestionEntity qe : questions){

            val inter = qe.getInteractions();
            if(inter != null) {
                ies.addAll(inter);
            }
        }

        Collections.sort(ies, new OrderComparator());
        Collections.reverse(ies);

        for (InteractionEntity ie : ies){
            ArrayList<MistakeEntity> mistakes = new ArrayList<>();
            if (ie.getMistakes() != null) {
                mistakes.addAll(ie.getMistakes());
            }

            for(MistakeEntity me : mistakes){
                if(conceptAtempt.containsKey(me.getLawName())){
                    conceptAtempt.get(me.getLawName()).add(false);
                }else{
                    ArrayList<Boolean> newLaw = new ArrayList<>();
                    newLaw.add(false);
                    conceptAtempt.put(me.getLawName(), newLaw);
                }
            }

            ArrayList<CorrectLawEntity> correctLaws = new ArrayList<>();
            if(ie.getCorrectLaw() != null) {
                correctLaws.addAll(ie.getCorrectLaw());
            }

            for(CorrectLawEntity cle : correctLaws){
                if(conceptAtempt.containsKey(cle.getLawName())){
                    conceptAtempt.get(cle.getLawName()).add(true);
                }else{
                    ArrayList<Boolean> newLaw = new ArrayList<>();
                    newLaw.add(true);
                    conceptAtempt.put(cle.getLawName(), newLaw);
                }
            }

        }

        for(HashMap.Entry<String, ArrayList<Boolean>> entry : conceptAtempt.entrySet()) {
            String key = entry.getKey();
            val value = entry.getValue();
            res.put(key, calculateWeightedScore(value));
        }

        return res;
    }

    private Float calculateWeightedScore(ArrayList<Boolean> value) {
        if(value.stream().count() == 0){
            return (float)0;
        }

        float trueCount = 0;
        float falseCount = 0;

        float signCount = 5;
        float curIteration = 0;

        for(boolean i: value){
            if(i){
                trueCount += 1;
            }else{
                falseCount += 1;
            }

            if(abs(curIteration-signCount) < 0.0001){
                break;
            }
        }

        return trueCount/(trueCount + falseCount);
    }

    class OrderComparator implements Comparator<InteractionEntity> {
        @Override
        public int compare(InteractionEntity a, InteractionEntity b) {
            return a.getOrderNumber() < b.getOrderNumber() ? -1 : a.getOrderNumber() == b.getOrderNumber() ? 0 : 1;
        }
    }
}
