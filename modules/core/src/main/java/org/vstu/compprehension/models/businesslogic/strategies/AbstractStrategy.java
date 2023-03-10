package org.vstu.compprehension.models.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.dto.ExerciseConceptDto;
import org.vstu.compprehension.dto.ExerciseLawDto;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.DisplayingFeedbackType;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface AbstractStrategy {
    @NotNull String getStrategyId();

    @NotNull StrategyOptions getOptions();

    QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt);

    DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question);

    FeedbackType determineFeedbackType(QuestionEntity question);

    /**
     * @param exerciseAttempt attempt to grade
     * @return grade in range [0..1]
     */
    float grade(ExerciseAttemptEntity exerciseAttempt);

    Decision decide(ExerciseAttemptEntity exerciseAttempt);

    @NotNull
    default List<Concept> filterExerciseStageConcepts(
            @NotNull List<ExerciseConceptDto> stageConcepts,
            Domain domain,
            RoleInExercise role) {
        return stageConcepts.stream().filter(ec -> ec.getKind().equals(role)).map(ec -> domain.getConcept(ec.getName())).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @NotNull
    default List<Concept> getExerciseStageConceptsWithChildren(
            @NotNull List<ExerciseConceptDto> stageConcepts,
            Domain domain,
            RoleInExercise role) {
        return stageConcepts.stream().filter(ec -> ec.getKind().equals(role)).flatMap(ec -> domain.getConceptWithChildren(ec.getName()).stream()).collect(Collectors.toList());
    }

    @NotNull
    default List<Law> getExerciseStageLawsWithImplied(
            @NotNull List<ExerciseLawDto> stageLaws,
            Domain domain,
            RoleInExercise role) {
        return stageLaws.stream()
                .filter(ec -> ec.getKind().equals(role))
                .flatMap(ec -> Stream.concat(
                        domain.getPositiveLawWithImplied(ec.getName()).stream(),
                        domain.getNegativeLawWithImplied(ec.getName()).stream()))
                .collect(Collectors.toList());
    }

    /**
     * Return names of questions were generated within an exercise attempt
     * @param exerciseAttempt attempt
     * @return list of plain names
     */
    default ArrayList<String> listQuestionNamesOfAttempt(ExerciseAttemptEntity exerciseAttempt) {
        ArrayList<String> deniedQuestions = new ArrayList<>();
        if (exerciseAttempt != null && exerciseAttempt.getQuestions() != null) {
            for (QuestionEntity q : exerciseAttempt.getQuestions()) {
                deniedQuestions.add(q.getQuestionName());
            }
        }
        return deniedQuestions;
    }
    /**
     * Return names of questions were generated within an exercise attempt
     * @param exerciseAttempt attempt
     * @return list {@link QuestionEntity} instances
     */
    default List<QuestionEntity> listQuestionsOfAttempt(ExerciseAttemptEntity exerciseAttempt) {
        ArrayList<QuestionEntity> questions = new ArrayList<>();
        if (exerciseAttempt != null && exerciseAttempt.getQuestions() != null) {
            questions.addAll(exerciseAttempt.getQuestions());
        }
        return questions;
    }


    /** Find exercise stage for next question in an attempt
     * @param exerciseAttempt attempt in progress
     * @return stage applied for next question generated for this attempt
     */
    default ExerciseStageEntity getStageForNextQuestion(ExerciseAttemptEntity exerciseAttempt) {
        if (exerciseAttempt == null || exerciseAttempt.getQuestions() == null) {
            return null;
        }

        List<ExerciseStageEntity> stages = exerciseAttempt.getExercise().getStages();
        int nQuestions = exerciseAttempt.getQuestions().size();
        int questionsInStagesCumulative = 0;

//        int nStages = stages.size();
//        int stageIndex = 0;
//        for (; stageIndex < nStages; ++stageIndex) {
//            st = stages.get(stageIndex);
        ExerciseStageEntity lastStage = null;
        for (ExerciseStageEntity currStage : stages) {
            questionsInStagesCumulative += currStage.getNumberOfQuestions();
            if (nQuestions < questionsInStagesCumulative) {  // not `<=` since we want `next` question
                return currStage;
            }
            lastStage = currStage;
        }
        // get last one if we go over the last stage
        return lastStage;
    }

    /** Get total number of questions defined by exercise stages
     * @param exercise exercise with stages
     * @return sum of stages' question counts
     */
    default int getNumberOfQuestionsToAsk(ExerciseEntity exercise) {
        if (exercise == null || exercise.getStages() == null) {
            return -1;
        }
        return exercise.getStages().stream().map(ExerciseStageEntity::getNumberOfQuestions).reduce(Integer::sum).orElse(0);
    }
}
