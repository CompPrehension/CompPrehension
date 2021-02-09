package com.example.demo.models.businesslogic;

import com.example.demo.Service.DomainService;
import com.example.demo.models.businesslogic.domains.Domain;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.DisplayingFeedbackType;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.QuestionStatus;
import com.example.demo.models.entities.EnumData.RoleInExercise;
import com.example.demo.utils.DomainAdapter;
import org.apache.jena.atlas.lib.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

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
        ArrayList<QuestionEntity> questions = new ArrayList<>();
        questions.addAll(exerciseAttempt.getQuestions());
        int questionCount = 0;
        float resolvedQuestionsGrade = 0; // За каждый вопрос можно назначать взвешенную оценку

        for(QuestionEntity qe : questions){
            questionCount++;
            if(qe.getQuestionStatus() == QuestionStatus.RESOLVED){//Пока за каждое решенное + 1
                resolvedQuestionsGrade++; //(самая простая стратегия - % решённых вопросов)
            }
        }
        return resolvedQuestionsGrade/(float)questionCount;
    }

    protected HashMap<String, Float> getLawGrade(ExerciseAttemptEntity exerciseAttempt){
        HashMap<String, Float> res = new HashMap<>();

        HashMap<String, ArrayList<Boolean>> conceptAtempt = new HashMap<>();
        ArrayList<QuestionEntity> questions = new ArrayList<>();
        questions.addAll(exerciseAttempt.getQuestions());

        ArrayList<InteractionEntity> ies = new ArrayList<>();
        for(QuestionEntity qe : questions){
            ies.addAll(qe.getInteractions());
        }

        Collections.sort(ies, new OrderComparator());
        Collections.reverse(ies);

        for (InteractionEntity ie : ies){
            ArrayList<MistakeEntity> mistakes = new ArrayList<>();
            mistakes.addAll(ie.getMistakes());

            for(MistakeEntity me : mistakes){
                if(conceptAtempt.containsKey(me.getLawName())){
                    conceptAtempt.get(me.getLawName()).add(false);
                }else{
                    ArrayList<Boolean> newLaw = new ArrayList<>();
                    newLaw.add(false);
                    conceptAtempt.put(me.getLawName(), newLaw);
                }
            }

            if(mistakes.stream().count() == 0){
                //выбрать верные законы и указать, что они были верны
            }
        }

        //пересчитать верные и неверные законы в число

        return res;
    }

    class OrderComparator implements Comparator<InteractionEntity> {
        @Override
        public int compare(InteractionEntity a, InteractionEntity b) {
            return a.getOrderNumber() < b.getOrderNumber() ? -1 : a.getOrderNumber() == b.getOrderNumber() ? 0 : 1;
        }
    }
}
