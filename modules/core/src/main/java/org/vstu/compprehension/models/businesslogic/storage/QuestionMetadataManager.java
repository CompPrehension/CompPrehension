package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Getter;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataBaseRepository;
import org.vstu.compprehension.utils.Checkpointer;

import java.util.*;

@Getter
public class QuestionMetadataManager {

    QuestionMetadataBaseRepository<? extends QuestionMetadataEntity> questionRepository;
    QuestionGroupStat wholeBankStat;
    HashMap<String, Long> conceptName2bit;
//    HashMap<String, Long> lawName2bit;
    HashMap<String, Long> violationName2bit;

    public QuestionMetadataManager(
            QuestionMetadataBaseRepository<? extends QuestionMetadataEntity>questionMetadataRepository
    ) {
        this.questionRepository = questionMetadataRepository;
//        this.questionRepository.;

        // test if it works
//        System.out.println("QuestionMetadataManager: begin listing...");
//        repository.findAll().forEach(qme -> System.out.println(qme.getId() + " - " + qme.getName()));
//        System.out.println("QuestionMetadataManager: end listing.");

        // use hardcoded name->bit mappings
        conceptName2bit = _fillConcepts(new HashMap<>());
        violationName2bit = _fillViolations(new HashMap<>());
        initBankStat();
    }

    public static long namesToBitmask(Collection<String> names, Map<String, Long> name2bitMapping) {
        return names.stream()
                .map(s -> name2bitMapping.getOrDefault(s, 0L))
                .reduce((a,b) -> a | b).orElse(0L);
    }

    private void initBankStat() {
        Checkpointer ch = new Checkpointer();
        ArrayList<QuestionMetadataEntity> allQuestions = new ArrayList<>();

        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAll();
        ch.hit("initBankStat - query ran");
        iter.forEach(allQuestions::add);
        ch.hit("initBankStat - query results collected");

        wholeBankStat = new QuestionGroupStat(allQuestions);
        ch.hit("initBankStat - stats prepared");
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
    List<QuestionMetadataEntity> findQuestionsByConcepts(Collection<Long> conceptBitEntries) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConcepts(conceptBitEntries);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }

    /** get entries unlimited */
    List<QuestionMetadataEntity> findQuestionsByConceptsLaws(Collection<Long> conceptBitEntries, Collection<Long> lawBitEntries) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptsLaws(conceptBitEntries, lawBitEntries);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }


    List<QuestionMetadataEntity> findQuestionsByConceptsWithoutTemplates(
            Collection<Long> conceptBitEntries,
            Collection<Integer> templatesIds
    ) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptsWithoutTemplates(conceptBitEntries, templatesIds);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }

    List<QuestionMetadataEntity> findQuestionsByConceptsLawsWithoutTemplates(
            Collection<Long> conceptBitEntries, Collection<Long> lawBitEntries,
            Collection<Integer> templatesIds
    ) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptsLawsWithoutTemplates(conceptBitEntries, lawBitEntries, templatesIds);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }

    List<QuestionMetadataEntity> findQuestionsByConceptEntriesLawBitmasksWithoutTemplates(
            Collection<Long> conceptBitEntries,
            Long lawRequiredBitmask, Long lawDeniedBitmask,
            Collection<Integer> templatesIds
    ) {
        lawRequiredBitmask &= ~lawDeniedBitmask;
        if (templatesIds == null || templatesIds.isEmpty()) {
            templatesIds = List.of(0);
        }
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptEntriesLawBitsWithoutTemplates(conceptBitEntries, lawRequiredBitmask, lawDeniedBitmask, templatesIds);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }

    List<QuestionMetadataEntity> findQuestionsByBitmasksWithoutTemplates(
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
    }

    private HashMap<String, Long> _fillConcepts(HashMap<String, Long> name2bit) {
        name2bit.put("pointer", 0x1L);
        name2bit.put("C++", 0x2L);
        name2bit.put("loops", 0x4L);
        name2bit.put("if/else", 0x8L);
        name2bit.put("expr:array", 0x10L);
        name2bit.put("expr:pointer", 0x20L);
        name2bit.put("expr:func_call", 0x40L);
        name2bit.put("expr:explicit_cast", 0x80L);
        name2bit.put("expr:class_member_access", 0x100L);
        name2bit.put("alternative", 0x200L);
        name2bit.put("else", 0x400L);
        name2bit.put("expr", 0x800L);
        name2bit.put("if", 0x1000L);
        name2bit.put("sequence", 0x2000L);
        name2bit.put("return", 0x4000L);
        name2bit.put("loop", 0x8000L);
        name2bit.put("while_loop", 0x10000L);
        name2bit.put("for_loop", 0x20000L);
        name2bit.put("else-if", 0x40000L);
        name2bit.put("nested_loop", 0x80000L);
        name2bit.put("do_while_loop", 0x100000L);
        name2bit.put("break", 0x200000L);
        name2bit.put("continue", 0x400000L);
        return name2bit;
    }
    private HashMap<String, Long> _fillViolations(HashMap<String, Long> name2bit) {
        name2bit.put("DuplicateOfAct", 0x1L);
        name2bit.put("ElseBranchAfterTrueCondition", 0x2L);
        name2bit.put("NoAlternativeEndAfterBranch", 0x4L);
        name2bit.put("NoBranchWhenConditionIsTrue", 0x8L);
        name2bit.put("NoFirstCondition", 0x10L);
        name2bit.put("SequenceFinishedTooEarly", 0x20L);
        name2bit.put("TooEarlyInSequence", 0x40L);
        name2bit.put("BranchOfFalseCondition", 0x80L);
        name2bit.put("LastConditionIsFalseButNoElse", 0x100L);
        name2bit.put("LastFalseNoEnd", 0x200L);
        name2bit.put("LoopStartIsNotCondition", 0x400L);
        name2bit.put("NoLoopEndAfterFailedCondition", 0x800L);
        name2bit.put("NoConditionAfterIteration", 0x1000L);
        name2bit.put("NoIterationAfterSuccessfulCondition", 0x2000L);
        name2bit.put("LoopStartIsNotIteration", 0x4000L);
        return name2bit;
    }

}
