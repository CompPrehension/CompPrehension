package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Getter;
import org.vstu.compprehension.models.businesslogic.storage.stats.NumericStat;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataBaseRepository;
import org.vstu.compprehension.utils.Checkpointer;

import java.math.BigInteger;
import java.util.*;

@Getter
public class QuestionMetadataManager {

    QuestionMetadataBaseRepository<? extends QuestionMetadataEntity> questionRepository;
    QuestionGroupStat wholeBankStat;
    HashMap<String, Long> conceptName2bit;
    HashMap<String, Long> lawName2bit;
    HashMap<String, Long> violationName2bit;

    public QuestionMetadataManager(
            QuestionMetadataBaseRepository<? extends QuestionMetadataEntity>questionMetadataRepository
    ) {
        this.questionRepository = questionMetadataRepository;

        initBankStat();
    }

    public static long namesToBitmask(Collection<String> names, Map<String, Long> name2bitMapping) {
        return names.stream()
                .map(s -> name2bitMapping.getOrDefault(s, 0L))
                .reduce((a,b) -> a | b).orElse(0L);
    }

    private void initBankStat() {
        Checkpointer ch = new Checkpointer();

        Map<String, Object> data = questionRepository.getStatOnComplexityField();
        NumericStat complStat = new NumericStat();
        complStat.setCount(((BigInteger)data.get("count")).intValue());
        complStat.setMin  ((Double) data.get("min"));
        complStat.setMean ((Double) data.get("mean"));
        complStat.setMax  ((Double) data.get("max"));

        wholeBankStat = new QuestionGroupStat();
        wholeBankStat.setComplexityStat(complStat);

//        ch.hit("initBankStat - stats prepared");
        ch.since_start("initBankStat - completed");
    }

//    /** get entries with limit */
//    List<QuestionMetadataBaseEntity> findQuestionsByConcepts(Collection<Integer> conceptBitEntries, int limit) {
//        ArrayList<QuestionMetadataBaseEntity> foundQuestions = new ArrayList<>();
//        Iterable<? extends QuestionMetadataBaseEntity> iter = questionRepository.findAllWithConcepts(conceptBitEntries, PageRequest.of(0, limit/*, Sort.by(Sort.Direction.ASC, "seatNumber")*/));
//        iter.forEach(foundQuestions::add);
//        return foundQuestions;
//    }

    /** get entries unlimited */
    /*List<QuestionMetadataEntity> findQuestionsByConcepts(Collection<Long> conceptBitEntries) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConcepts(conceptBitEntries);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }*/

    /** get entries unlimited */
    /*List<QuestionMetadataEntity> findQuestionsByConceptsLaws(Collection<Long> conceptBitEntries, Collection<Long> lawBitEntries) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptsLaws(conceptBitEntries, lawBitEntries);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }*/


    /*List<QuestionMetadataEntity> findQuestionsByConceptsWithoutTemplates(
            Collection<Long> conceptBitEntries,
            Collection<Integer> templatesIds
    ) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptsWithoutTemplates(conceptBitEntries, templatesIds);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }*/

    /*List<QuestionMetadataEntity> findQuestionsByConceptsLawsWithoutTemplates(
            Collection<Long> conceptBitEntries, Collection<Long> lawBitEntries,
            Collection<Integer> templatesIds
    ) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptsLawsWithoutTemplates(conceptBitEntries, lawBitEntries, templatesIds);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }*/

    /*List<QuestionMetadataEntity> findQuestionsByConceptEntriesLawBitmasksWithoutTemplates(
            Collection<Long> traceConceptBitEntries, long deniedConceptsBitmask,
            Long lawRequiredBitmask, Long lawDeniedBitmask,
            Collection<Integer> templatesIds
    ) {
        lawRequiredBitmask &= ~lawDeniedBitmask;
        if (templatesIds == null || templatesIds.isEmpty()) {
            templatesIds = List.of(0);
        }
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptEntriesLawBitsWithoutTemplates(traceConceptBitEntries, deniedConceptsBitmask, lawRequiredBitmask, lawDeniedBitmask, templatesIds);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }*/

    /*List<QuestionMetadataEntity> findQuestionsByBitmasksWithoutTemplates(
            Long conceptRequiredBitmask, Long conceptDeniedBitmask,
            Long lawRequiredBitmask, Long lawDeniedBitmask,
            Collection<Integer> templatesIds
    ) {
        conceptRequiredBitmask &= ~conceptDeniedBitmask;
        lawRequiredBitmask &= ~lawDeniedBitmask;
        if (templatesIds == null || templatesIds.isEmpty()) {
            templatesIds = List.of(0);
        }
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptLawBitsWithoutTemplates(conceptRequiredBitmask, conceptDeniedBitmask, lawRequiredBitmask, lawDeniedBitmask, templatesIds);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }*/

    List<QuestionMetadataEntity> findQuestionsAroundComplexityStepsWithoutTemplates(
            double complexity, int solutionSteps,
            Long conceptPreferredBitmask, Long conceptDeniedBitmask,
            Long lawPreferredBitmask, Long lawDeniedBitmask,
            Collection<Integer> templatesIds,
            int limit
    ) {
        conceptPreferredBitmask &= ~conceptDeniedBitmask;
        lawPreferredBitmask &= ~lawDeniedBitmask;
        if (templatesIds == null || templatesIds.isEmpty()) {
            templatesIds = List.of(0);
        }
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findSampleAroundComplexityStepsWithoutTemplates(complexity, solutionSteps, conceptPreferredBitmask, conceptDeniedBitmask, lawPreferredBitmask, lawDeniedBitmask, templatesIds, limit);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }

    /**
     * @param complexity preferred complexity value
     * @param conceptPreferredBitmask bits of concepts that should present (at least one is required)
     * @param conceptDeniedBitmask concept bits to exclude hard
     * @param lawPreferredBitmask bits of laws that should present (at least one is required)
     * @param lawDeniedBitmask law bits to exclude hard
     * @param templatesIds IDs of templates to exclude hard
     * @param limit limit query results to first `limit` records
     * @param randomPoolLimit size of random pool, must be >= limit
     * @return found question-metadata entries
     */
    List<QuestionMetadataEntity> findQuestionsAroundComplexityWithoutTemplates(
            double complexity,
            double complexityMaxDifference,
            int solutionStepsMin,
            int solutionStepsMax,
            Long conceptPreferredBitmask, Long conceptDeniedBitmask,
            Long lawPreferredBitmask, Long lawDeniedBitmask,
            Collection<Integer> templatesIds,
            Collection<Integer> questionsIds,
            int limit,
            int randomPoolLimit
    ) {
        conceptPreferredBitmask &= ~conceptDeniedBitmask;
        lawPreferredBitmask &= ~lawDeniedBitmask;
        // lists cannot be empty in SQL: workaround
        if (templatesIds == null || templatesIds.isEmpty()) {
            templatesIds = List.of(0);
        }
        if (questionsIds == null || questionsIds.isEmpty()) {
            questionsIds = List.of(0);
        }
        if (randomPoolLimit < limit)
            randomPoolLimit = limit;
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findSampleAroundComplexityWithoutTemplates(complexity, complexityMaxDifference,
                solutionStepsMin, solutionStepsMax,
                conceptPreferredBitmask, conceptDeniedBitmask, lawPreferredBitmask, lawDeniedBitmask, templatesIds, questionsIds, limit, randomPoolLimit);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }
}
