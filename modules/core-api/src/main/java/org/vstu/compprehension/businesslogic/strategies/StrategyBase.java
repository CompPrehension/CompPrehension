package org.vstu.compprehension.businesslogic.strategies;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.RoleInExercise;
import org.vstu.compprehension.enums.SearchDirections;
import org.vstu.compprehension.frontend.dto.ExerciseConceptDto;
import org.vstu.compprehension.frontend.dto.ExerciseLawDto;
import org.vstu.compprehension.frontend.dto.ExerciseSkillDto;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.settings.SettingsType;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptStageData;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class StrategyBase implements AbstractStrategy {

    private final @NotNull SettingsType<?> settingsType;

    protected StrategyBase(@NotNull SettingsType<?> settingsType) {
        this.settingsType = settingsType;
    }

    @Override
    public @NotNull SettingsType<?> getSettingsType() {
        return settingsType;
    }

    @NotNull
    protected List<Concept> filterExerciseStageConcepts(
            @NotNull List<ExerciseConceptDto> stageConcepts,
            Domain domain,
            RoleInExercise role) {
        return stageConcepts.stream().filter(ec -> ec.getKind().equals(role)).map(ec -> domain.getConcept(ec.getName())).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @NotNull
    protected List<Concept> getExerciseStageConceptsWithChildren(
            @NotNull List<ExerciseConceptDto> stageConcepts,
            Domain domain,
            RoleInExercise role) {
        return stageConcepts.stream().filter(ec -> ec.getKind().equals(role)).flatMap(ec -> domain.getConceptWithChildren(ec.getName()).stream()).collect(Collectors.toList());
    }

    @NotNull
    protected List<Law> getExerciseStageLawsWithImplied(
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

    @NotNull
    protected List<Skill> getExerciseStageSkillsWithImplied(
            @NotNull List<ExerciseSkillDto> stageSkills,
            Domain domain,
            RoleInExercise role) {
        return stageSkills.stream()
                .filter(ec -> ec.getKind().equals(role))
                .map(ec -> domain.getSkill(ec.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Return names of questions were generated within an exercise attempt
     * @param exerciseAttempt attempt
     * @return list of plain names
     */
    protected ArrayList<String> listQuestionNamesOfAttempt(ExerciseAttemptWithQuestionsData exerciseAttempt) {
        ArrayList<String> deniedQuestions = new ArrayList<>();
        if (exerciseAttempt != null && exerciseAttempt.questions() != null) {
            for (AttemptQuestionData q : exerciseAttempt.questions()) {
                String questionName = q.name();
                if (questionName != null) {
                    deniedQuestions.add(questionName);
                }
            }
        }
        return deniedQuestions;
    }

    /** Fill target and denied concepts and laws, complexity and denied questions from the stage of the attempt */
    protected QuestionRequest initQuestionRequest(AttemptStageData attemptStage, Domain domain) {
        var exerciseAttempt = attemptStage.getAttempt();
        var exerciseStage = attemptStage.getStage();
        List<ExerciseConceptDto> exConcepts = exerciseStage.getConcepts();
        Set<Concept> allowed = new HashSet<>(getExerciseStageConceptsWithChildren(exConcepts, domain, RoleInExercise.PERMITTED));
        allowed.addAll(exConcepts.stream().filter(ec -> ec.getKind().equals(RoleInExercise.TARGETED)).flatMap(ec -> domain.getChildrenOfConcept(ec.getName()).stream()).collect(Collectors.toSet()));

        List<ExerciseLawDto> exLaws = exerciseStage.getLaws();

        return QuestionRequest.builder()
                .exerciseAttemptId(exerciseAttempt.id())
                .domainShortname(domain.getDomainId())
                // concepts
                .targetConcepts(filterExerciseStageConcepts(exConcepts, domain, RoleInExercise.TARGETED))
                .allowedConcepts(List.copyOf(allowed))
                .deniedConcepts(getExerciseStageConceptsWithChildren(exConcepts, domain, RoleInExercise.FORBIDDEN))
                // laws
                .targetLaws(getExerciseStageLawsWithImplied(exLaws, domain, RoleInExercise.TARGETED))
                .allowedLaws(getExerciseStageLawsWithImplied(exLaws, domain, RoleInExercise.PERMITTED))
                .deniedLaws(getExerciseStageLawsWithImplied(exLaws, domain, RoleInExercise.FORBIDDEN))
                // tags
                .targetTags(exerciseAttempt.exercise().tags().stream().map(t -> domain.getTags().get(t)).filter(Objects::nonNull).toList())
                // skills
                .targetSkills(getExerciseStageSkillsWithImplied(exerciseStage.getSkills(), domain, RoleInExercise.TARGETED))
                .allowedSkills(getExerciseStageSkillsWithImplied(exerciseStage.getSkills(), domain, RoleInExercise.PERMITTED))
                .deniedSkills(getExerciseStageSkillsWithImplied(exerciseStage.getSkills(), domain, RoleInExercise.FORBIDDEN))
                // questions
                .deniedQuestionNames(listQuestionNamesOfAttempt(exerciseAttempt))
                // deny individual questions only
                .deniedQuestionMetaIds(exerciseAttempt.questions().stream()
                        .filter(q -> q.metadata() != null)
                        .map(q -> q.metadata().id())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .complexitySearchDirection(SearchDirections.TO_SIMPLE)
                .lawsSearchDirection(SearchDirections.TO_SIMPLE)
                .chanceToPickAutogeneratedQuestion(1.0)
                .complexity(exerciseStage.getComplexity())  // [0..1], copy as is
                .build();
    }

    /** Balance targets (concepts and laws) so to show as many of questions with them as possible within the stage */
    protected QuestionRequest adjustQuestionRequest(QuestionRequest qr, AttemptStageData attemptStage) {

        val attemptQuestions = attemptStage.getQuestions();
        if (attemptQuestions.isEmpty()) {
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

            qr = qr.toBuilder().targetConcepts(new ArrayList<>(targetConcepts)).build();
        }


        // Balance laws ...

        Set<Law> targetLaws = new HashSet<>(qr.getTargetLaws());
        Set<Law> deniedLaws = new HashSet<>(qr.getDeniedLaws());

        // guard: don't allow overlapping of target & denied
        targetLaws.removeAll(deniedLaws);

        if (targetLaws.size() >= 2) {
            targetLaws = leastUsedViolations(targetLaws, attemptQuestions, 0.0);

            qr = qr.toBuilder().targetLaws(new ArrayList<>(targetLaws)).build();
        }


        return qr;
    }

    protected Set<Concept> leastUsedConcepts(Set<Concept> targetConcepts, List<AttemptQuestionData> attemptQuestions, double leastUsedRatio) {
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
            val m = q.metadata();
            if (m == null)
                continue;
            long bits = m.conceptsSatisfiedFromPlan() & currentTargetConceptBits;
            // count bits in previous questions, where common concepts were targeted

            incrementCountMap(satisfied, bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            bits = m.conceptsUnsatisfiedFromPlan() & currentTargetConceptBits;
            incrementCountMap(unsatisfied, bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            if (m.conceptsSatisfiedFromRequest() == 0) {
                // nothing was found on that try => no more tries (to request this concept only)
                bits = m.conceptBitsInRequest() & currentTargetConceptBits;
                unreachable.addAll(bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            }
        }

        return leastFrequent(targetConcepts, satisfied, unsatisfied, leastUsedRatio, unreachable);
    }

    protected Set<Law> leastUsedViolations(Set<Law> targetLaws, List<AttemptQuestionData> attemptQuestions, double leastUsedRatio) {
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
            val m = q.metadata();
            if (m == null)
                continue;
            long bits = m.violationsSatisfiedFromPlan() & currentTargetLawBits;
            // count bits in previous questions, where common laws were targeted

            incrementCountMap(satisfied, bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            bits = m.violationsUnsatisfiedFromPlan() & currentTargetLawBits;
            incrementCountMap(unsatisfied, bitmask2singleBits(bits).stream().map(bit2thing::get).collect(Collectors.toSet()));
            if (m.violationsSatisfiedFromRequest() == 0) {
                // nothing was found on that try => no more tries (to request this law only)
                bits = m.violationBitsInRequest() & currentTargetLawBits;
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

    protected static List<Long> bitmask2singleBits(long mask) {
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

    protected <T> void incrementCountMap(Map<T, Integer> obj2count, Collection<T> objects) {
        if (objects != null) {
            objects.forEach(t -> obj2count.merge(t, 1, Integer::sum));
        }
    }
}
