package org.vstu.compprehension.models.businesslogic.strategies;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.dto.ExerciseConceptDto;
import org.vstu.compprehension.dto.ExerciseLawDto;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.EnumData.*;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;

import java.util.*;
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

    /** Fill target and denied concepts and laws, complexity and denied questions from the attempt */
    default QuestionRequest initQuestionRequest(ExerciseAttemptEntity exerciseAttempt, ExerciseStageEntity exerciseStage, Domain domain) {
        QuestionRequest qr = new QuestionRequest();
        qr.setExerciseAttempt(exerciseAttempt);
        qr.setDomainShortname(domain.getShortName());

        // concepts
        List<ExerciseConceptDto> exConcepts = exerciseStage.getConcepts();
        // take target concepts as is, use their children as `allowed`
        qr.setTargetConcepts (filterExerciseStageConcepts(exConcepts, domain, RoleInExercise.TARGETED));

        Set<Concept> allowed = new HashSet<>(getExerciseStageConceptsWithChildren(exConcepts, domain, RoleInExercise.PERMITTED));
        allowed.addAll(exConcepts.stream().filter(ec -> ec.getKind().equals(RoleInExercise.TARGETED)).flatMap(ec -> domain.getChildrenOfConcept(ec.getName()).stream()).collect(Collectors.toSet()));
        qr.setAllowedConcepts(List.copyOf(allowed));

        qr.setDeniedConcepts (getExerciseStageConceptsWithChildren(exConcepts, domain, RoleInExercise.FORBIDDEN));

        // laws
        List<ExerciseLawDto> exLaws = exerciseStage.getLaws();
        qr.setTargetLaws(getExerciseStageLawsWithImplied(exLaws, domain, RoleInExercise.TARGETED));
        qr.setAllowedLaws(getExerciseStageLawsWithImplied(exLaws, domain, RoleInExercise.PERMITTED));
        qr.setDeniedLaws(getExerciseStageLawsWithImplied(exLaws, domain, RoleInExercise.FORBIDDEN));

        // questions
        qr.setDeniedQuestionNames(listQuestionNamesOfAttempt(exerciseAttempt));

        // don't deny other questions from templates shown
        qr.setDeniedQuestionTemplateIds(List.of(0));
        /*qr.setDeniedQuestionTemplateIds(listQuestionsOfAttempt(exerciseAttempt).stream().map(q -> q.getOptions().getTemplateId()).filter(id -> id != -1).collect(Collectors.toList()));*/

        // deny individual questions only
        qr.setDeniedQuestionMetaIds(listQuestionsOfAttempt(exerciseAttempt).stream().map(q -> q.getOptions().getQuestionMetaId()).filter(id -> id > 0).collect(Collectors.toList()));

        qr.setComplexitySearchDirection(SearchDirections.TO_SIMPLE);
        qr.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
        qr.setChanceToPickAutogeneratedQuestion(1.0);
        qr.setComplexity(exerciseAttempt.getExercise().getComplexity());  // [0..1], copy as is

        return qr;
    }

    /** Balance targets (concepts and laws) so to show as many of questions with them as possible within the attempt */
    default QuestionRequest adjustQuestionRequest(QuestionRequest qr, ExerciseAttemptEntity exerciseAttempt) {

        // copy targets to "inPlan" fields, actual targets may change
        qr.setTargetConceptsInPlan(qr.getTargetConcepts());
        qr.setTargetLawsInPlan(qr.getTargetLaws());

        val attemptQuestions = exerciseAttempt.getQuestions();
        if (attemptQuestions == null || attemptQuestions.isEmpty()) {
            // don't change targets since nothing shown yet
            return qr;
        }

        // Balance concepts ...

        Set<Concept> targetConcepts = new HashSet<>(qr.getTargetConcepts());
        Set<Concept> deniedConcepts = new HashSet<>(qr.getDeniedConcepts());

        // guard: don't allow overlapping of target & denied
        targetConcepts.removeAll(deniedConcepts);

        if (targetConcepts.size() >= 2) {
            targetConcepts = leastUsedConcepts(targetConcepts, attemptQuestions, 0.0);

            qr.setTargetConcepts(new ArrayList<>(targetConcepts));
        }


        // Balance laws ...

        Set<Law> targetLaws = new HashSet<>(qr.getTargetLaws());
        Set<Law> deniedLaws = new HashSet<>(qr.getDeniedLaws());

        // guard: don't allow overlapping of target & denied
        targetLaws.removeAll(deniedLaws);

        if (targetLaws.size() >= 2) {
            targetLaws = leastUsedViolations(targetLaws, attemptQuestions, 0.0);

            qr.setTargetLaws(new ArrayList<>(targetLaws));
        }


        return qr;
    }

    default Set<Concept> leastUsedConcepts(Set<Concept> targetConcepts, List<QuestionEntity> attemptQuestions, double leastUsedRatio) {
        if (attemptQuestions.isEmpty())
            return targetConcepts;

        // consider only ones having a bitmask

        HashMap<Long, Concept> bit2thing = new HashMap<>();
        // map all bits of sub-concepts to this "enclosing" target concept
        for (Concept t : targetConcepts) {
            bitmask2singleBits(t.getSubTreeBitmask()).forEach(bit -> bit2thing.put(bit, t));
        }

        long currentTargetConceptBits = bit2thing.keySet().stream()
                .reduce((a,b) -> a|b).orElse(0L);
        /*long currentTargetConceptBits = targetConcepts.stream().mapToLong(TreeNodeWithBitmask::getSubTreeBitmask).reduce((a,b) -> a|b).orElse(0);*/

        HashMap<Concept, Integer> satisfied = new HashMap<>();  // concept -> count
        HashMap<Concept, Integer> unsatisfied = new HashMap<>();
        var unreachable = new HashSet<Concept>();  // concepts shouldn't be tried since are blocked by "denied"s in current context

        for (val q : attemptQuestions) {
            val m = q.getOptions().getMetadata();
            if (m == null)
                continue;
            long bits = m.traceConceptsSatisfiedFromPlan() & currentTargetConceptBits;
            // count bits in previous questions, where common concepts were targeted

            incrementCountMap(satisfied, bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            bits = m.traceConceptsUnsatisfiedFromPlan() & currentTargetConceptBits;
            incrementCountMap(unsatisfied, bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            if (m.traceConceptsSatisfiedFromRequest() == 0) {
                // nothing was found on that try => no more tries (to request this concept only)
                bits = m.getConceptBitsInRequest() & currentTargetConceptBits;
                unreachable.addAll(bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            }
        }

        return leastFrequent(targetConcepts, satisfied, unsatisfied, leastUsedRatio, unreachable);
    }

    default Set<Law> leastUsedViolations(Set<Law> targetLaws, List<QuestionEntity> attemptQuestions, double leastUsedRatio) {
        if (attemptQuestions.isEmpty())
            return targetLaws;

        // consider only ones having a bitmask

        HashMap<Long, Law> bit2thing = new HashMap<>();
        // map all bits of sub-laws to this "enclosing" target law
        for (Law t : targetLaws) {
            bitmask2singleBits(t.getSubTreeBitmask()).forEach(bit -> bit2thing.put(bit, t));
        }

        long currentTargetLawBits = bit2thing.keySet().stream()
                .reduce((a,b) -> a|b).orElse(0L);

        HashMap<Law, Integer> satisfied = new HashMap<>();  // concept -> count
        HashMap<Law, Integer> unsatisfied = new HashMap<>();
        var unreachable = new HashSet<Law>();  // laws shouldn't be tried since are blocked by "denied"s in current context

        for (val q : attemptQuestions) {
            val m = q.getOptions().getMetadata();
            if (m == null)
                continue;
            long bits = m.violationsSatisfiedFromPlan() & currentTargetLawBits;
            // count bits in previous questions, where common laws were targeted

            incrementCountMap(satisfied, bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            bits = m.violationsUnsatisfiedFromPlan() & currentTargetLawBits;
            incrementCountMap(unsatisfied, bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            if (m.violationsSatisfiedFromRequest() == 0) {
                // nothing was found on that try => no more tries (to request this law only)
                bits = m.getViolationBitsInRequest() & currentTargetLawBits;
                unreachable.addAll(bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            }
        }

        return leastFrequent(targetLaws, satisfied, unsatisfied, leastUsedRatio, unreachable);
    }

    private static <T> Set<T> leastFrequent(Set<T> currentTargets, HashMap<T, Integer> satisfied, HashMap<T, Integer> unsatisfied, double leastUsedRatio, Set<T> unreachable) {
        Set<T> allTargetsSet = new HashSet<>(satisfied.keySet());
        allTargetsSet.addAll(unsatisfied.keySet());

        if (allTargetsSet.isEmpty())
            return currentTargets;

        Set<T> allTargets = new HashSet<>(allTargetsSet);

        allTargets.removeAll(unreachable);
        // don't alter allTargetsSet but filter it when used below

        if (allTargets.isEmpty())
            return currentTargets;

        Set<T> unseenTargets = new HashSet<>(currentTargets);
        unseenTargets.removeAll(allTargets);

        HashMap<T, Double> ratios = new HashMap<>();  // a concept -> relative frequency
        for (T t : allTargetsSet) {
            if (!allTargets.contains(t)) // filer out targets removed from allTargets
                continue;
            int sat = satisfied.getOrDefault(t, 0);
            int unsat = unsatisfied.getOrDefault(t, 0);
            double ratio = sat / (double) (sat + unsat);
            ratios.put(t, ratio);
        }

        val minVal = ratios.values().stream().min(Double::compareTo).orElse(1d);
        val threshold = Math.nextUp(minVal /*+ leastUsedRatio * (maxVal - minVal)*/);  // ignore since we assume leastUsedRatio == 0 so far
        Set<T> resultTargets = unseenTargets;  // ! do not forget unfiltered bits
        for (T t: ratios.keySet()) {
            if (ratios.get(t) <= threshold) {
                resultTargets.add(t);
            }
        }
        return resultTargets;
    }

    static List<Long> bitmask2singleBits(long mask) {
        if (mask == 0)
            return List.of();
            
        val singleBits = new ArrayList<Long>();
        val bs = BitSet.valueOf(new long[]{mask});
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
            singleBits.add(1L << i);
            if (i == Integer.MAX_VALUE) break; // or (i+1) would overflow
        }
        return singleBits;
    }

    default <T> void incrementCountMap(Map<T, Integer> obj2count, Collection<T> objects) {
        if (objects != null) {
            objects.forEach(t -> obj2count.merge(t, 1, Integer::sum));
        }
    }

}
