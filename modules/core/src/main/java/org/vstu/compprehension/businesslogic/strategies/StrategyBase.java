package org.vstu.compprehension.businesslogic.strategies;

import org.vstu.compprehension.data.exercise.ExerciseStageData;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.enums.RoleInExercise;
import org.vstu.compprehension.data.enums.SearchDirections;
import org.vstu.compprehension.services.ExerciseAttemptService;
import org.vstu.compprehension.dto.ExerciseConceptDto;
import org.vstu.compprehension.dto.ExerciseLawDto;
import org.vstu.compprehension.dto.ExerciseSkillDto;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.data.exerciseattempt.AttemptExerciseData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Общая реализация стратегий: доступ к данным попытки и разделяемые помощники.
 * <p>
 * {@link AbstractStrategy} принимает только идентификаторы, чтобы контракт не приходилось
 * менять каждый раз, когда одной из стратегий понадобились новые данные. Достаёт данные
 * этот класс — через сервисы, и только те, что нужны конкретной стратегии.
 */
public abstract class StrategyBase implements AbstractStrategy {

    private final ExerciseAttemptService exerciseAttemptService;

    protected StrategyBase(ExerciseAttemptService exerciseAttemptService) {
        this.exerciseAttemptService = exerciseAttemptService;
    }

    /**
     * Данные попытки: упражнение, вопросы, взаимодействия.
     * <p>
     * Возвращает отсоединённые от Hibernate данные, а не сущность: стратегии не должны
     * ни ходить по ленивым связям, ни зависеть от того, открыта ли сессия. Сервис
     * собирает это фиксированным числом запросов, так что скрытых N+1 здесь нет.
     */
    protected @NotNull ExerciseAttemptWithQuestionsData getAttempt(long exerciseAttemptId) {
        return exerciseAttemptService.getAttemptWithQuestions(exerciseAttemptId);
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

    /** Find exercise stage for next question in an attempt
     * @param exerciseAttempt attempt in progress
     * @return stage applied for next question generated for this attempt
     */
    protected ExerciseStageData getStageForNextQuestion(ExerciseAttemptWithQuestionsData exerciseAttempt) {
        if (exerciseAttempt == null || exerciseAttempt.questions() == null) {
            return null;
        }

        List<ExerciseStageData> stages = exerciseAttempt.exercise().stages();
        int nQuestions = exerciseAttempt.questions().size();
        int questionsInStagesCumulative = 0;

//        int nStages = stages.size();
//        int stageIndex = 0;
//        for (; stageIndex < nStages; ++stageIndex) {
//            st = stages.get(stageIndex);
        ExerciseStageData lastStage = null;
        for (ExerciseStageData currStage : stages) {
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
    protected int getNumberOfQuestionsToAsk(AttemptExerciseData exercise) {
        if (exercise == null || exercise.stages() == null) {
            return -1;
        }
        return exercise.stages().stream().map(ExerciseStageData::getNumberOfQuestions).reduce(Integer::sum).orElse(0);
    }

    /** Fill target and denied concepts and laws, complexity and denied questions from the attempt */
    protected QuestionRequest initQuestionRequest(ExerciseAttemptWithQuestionsData exerciseAttempt, ExerciseStageData exerciseStage, Domain domain) {
        QuestionRequest qr = new QuestionRequest();
        qr.setExerciseAttemptId(exerciseAttempt.id());
        // // qr.setDomainShortname(domain.getShortName());
        qr.setDomainShortname(domain.getShortnameForQuestionSearch());

        // concepts
        List<ExerciseConceptDto> exConcepts = exerciseStage.getConcepts();
        qr.setTargetConcepts(filterExerciseStageConcepts(exConcepts, domain, RoleInExercise.TARGETED));

        Set<Concept> allowed = new HashSet<>(getExerciseStageConceptsWithChildren(exConcepts, domain, RoleInExercise.PERMITTED));
        allowed.addAll(exConcepts.stream().filter(ec -> ec.getKind().equals(RoleInExercise.TARGETED)).flatMap(ec -> domain.getChildrenOfConcept(ec.getName()).stream()).collect(Collectors.toSet()));
        qr.setAllowedConcepts(List.copyOf(allowed));

        qr.setDeniedConcepts(getExerciseStageConceptsWithChildren(exConcepts, domain, RoleInExercise.FORBIDDEN));

        // laws
        List<ExerciseLawDto> exLaws = exerciseStage.getLaws();
        qr.setTargetLaws(getExerciseStageLawsWithImplied(exLaws, domain, RoleInExercise.TARGETED));
        qr.setAllowedLaws(getExerciseStageLawsWithImplied(exLaws, domain, RoleInExercise.PERMITTED));
        qr.setDeniedLaws(getExerciseStageLawsWithImplied(exLaws, domain, RoleInExercise.FORBIDDEN));

        // tags
        qr.setTargetTags(exerciseAttempt.exercise().tags().stream().map(t -> domain.getTags().get(t)).filter(Objects::nonNull).toList());

        // skills
        qr.setTargetSkills(getExerciseStageSkillsWithImplied(exerciseStage.getSkills(), domain, RoleInExercise.TARGETED));
        qr.setAllowedSkills(getExerciseStageSkillsWithImplied(exerciseStage.getSkills(), domain, RoleInExercise.PERMITTED));
        qr.setDeniedSkills(getExerciseStageSkillsWithImplied(exerciseStage.getSkills(), domain, RoleInExercise.FORBIDDEN));

        // questions
        qr.setDeniedQuestionNames(listQuestionNamesOfAttempt(exerciseAttempt));

        // deny individual questions only
        qr.setDeniedQuestionMetaIds(exerciseAttempt.questions().stream()
                .filter(q -> q.metadata() != null)
                .map(q -> q.metadata().id())
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        qr.setComplexitySearchDirection(SearchDirections.TO_SIMPLE);
        qr.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
        qr.setChanceToPickAutogeneratedQuestion(1.0);
        qr.setComplexity(exerciseStage.getComplexity());  // [0..1], copy as is

        return qr;
    }

    /** Balance targets (concepts and laws) so to show as many of questions with them as possible within the attempt */
    protected QuestionRequest adjustQuestionRequest(QuestionRequest qr, ExerciseAttemptWithQuestionsData exerciseAttempt) {

        val attemptQuestions = exerciseAttempt.questions();
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
