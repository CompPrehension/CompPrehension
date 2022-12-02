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
    HashMap<String, Integer> conceptName2bit;
    HashMap<String, Integer> lawName2bit;

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
        lawName2bit = _fillViolations(new HashMap<>());
        initBankStat();
    }

    public static int namesToBitmask(Collection<String> names, Map<String, Integer> name2bitMapping) {
        return names.stream()
                .map(s -> name2bitMapping.getOrDefault(s, 0))
                .reduce((a,b) -> a | b).orElse(0);
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
        ch.since_start("initBankStat - completed", false);
    }

//    /** get entries with limit */
//    List<QuestionMetadataBaseEntity> findQuestionsByConcepts(Collection<Integer> conceptBitEntries, int limit) {
//        ArrayList<QuestionMetadataBaseEntity> foundQuestions = new ArrayList<>();
//        Iterable<? extends QuestionMetadataBaseEntity> iter = questionRepository.findAllWithConcepts(conceptBitEntries, PageRequest.of(0, limit/*, Sort.by(Sort.Direction.ASC, "seatNumber")*/));
//        iter.forEach(foundQuestions::add);
//        return foundQuestions;
//    }

    /** get entries unlimited */
    List<QuestionMetadataEntity> findQuestionsByConcepts(Collection<Integer> conceptBitEntries) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConcepts(conceptBitEntries);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }

    List<QuestionMetadataEntity> findQuestionsByConceptsWithoutTemplates(
            Collection<Integer> conceptBitEntries,
            Collection<Integer> templatesIds
    ) {
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findAllWithConceptsWithoutTemplates(conceptBitEntries, templatesIds);
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }

    private HashMap<String, Integer> _fillConcepts(HashMap<String, Integer> name2bit) {
        name2bit.put("pointer", 0x1);
        name2bit.put("C++", 0x2);
        name2bit.put("loops", 0x4);
        name2bit.put("if/else", 0x8);
        name2bit.put("expr:array", 0x10);
        name2bit.put("expr:pointer", 0x20);
        name2bit.put("expr:func_call", 0x40);
        name2bit.put("expr:explicit_cast", 0x80);
        name2bit.put("expr:class_member_access", 0x100);
        name2bit.put("alternative", 0x200);
        name2bit.put("else", 0x400);
        name2bit.put("expr", 0x800);
        name2bit.put("if", 0x1000);
        name2bit.put("sequence", 0x2000);
        name2bit.put("return", 0x4000);
        name2bit.put("loop", 0x8000);
        name2bit.put("while_loop", 0x10000);
        name2bit.put("for_loop", 0x20000);
        name2bit.put("else-if", 0x40000);
        name2bit.put("nested_loop", 0x80000);
        name2bit.put("do_while_loop", 0x100000);
        name2bit.put("break", 0x200000);
        name2bit.put("continue", 0x400000);
        return name2bit;
    }
    private HashMap<String, Integer> _fillViolations(HashMap<String, Integer> name2bit) {
        name2bit.put("DuplicateOfAct", 0x1);
        name2bit.put("ElseBranchAfterTrueCondition", 0x2);
        name2bit.put("NoAlternativeEndAfterBranch", 0x4);
        name2bit.put("NoBranchWhenConditionIsTrue", 0x8);
        name2bit.put("NoFirstCondition", 0x10);
        name2bit.put("SequenceFinishedTooEarly", 0x20);
        name2bit.put("TooEarlyInSequence", 0x40);
        name2bit.put("BranchOfFalseCondition", 0x80);
        name2bit.put("LastConditionIsFalseButNoElse", 0x100);
        name2bit.put("LastFalseNoEnd", 0x200);
        name2bit.put("LoopStartIsNotCondition", 0x400);
        name2bit.put("NoLoopEndAfterFailedCondition", 0x800);
        name2bit.put("NoConditionAfterIteration", 0x1000);
        name2bit.put("NoIterationAfterSuccessfulCondition", 0x2000);
        name2bit.put("LoopStartIsNotIteration", 0x4000);
        return name2bit;
    }

}
