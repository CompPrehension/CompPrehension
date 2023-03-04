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
//        this.questionRepository.;

        // test if it works
//        System.out.println("QuestionMetadataManager: begin listing...");
//        repository.findAll().forEach(qme -> System.out.println(qme.getId() + " - " + qme.getName()));
//        System.out.println("QuestionMetadataManager: end listing.");

        // use hardcoded name->bit mappings
        conceptName2bit = _fillConcepts(new HashMap<>());
        lawName2bit = _fillLaws(new HashMap<>());
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
     * @param randomPoolMultiplier how many times bigger than `limit` the random pool should be, must be >= 1 (normally 2..5)
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
            int limit,
            double randomPoolMultiplier
    ) {
        conceptPreferredBitmask &= ~conceptDeniedBitmask;
        lawPreferredBitmask &= ~lawDeniedBitmask;
        if (templatesIds == null || templatesIds.isEmpty()) {
            templatesIds = List.of(0);
        }
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findSampleAroundComplexityWithoutTemplates(complexity, complexityMaxDifference,
                solutionStepsMin, solutionStepsMax,
                conceptPreferredBitmask, conceptDeniedBitmask, lawPreferredBitmask, lawDeniedBitmask, templatesIds, limit, (int)(limit * randomPoolMultiplier));
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }

    private HashMap<String, Long> _fillConcepts(HashMap<String, Long> name2bit) {
        // control Flow
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
                 //   stmt       0x800000L
        name2bit.put("seq_longer_than1", 0x1000000L);
        // Expression
        name2bit.put("operator", 0x1L);  	// (1)
        name2bit.put("operator_,", 0x2L);  	// (2)
        name2bit.put("operator_==", 0x4L);  	// (4)
        name2bit.put("operator_!", 0x8L);  	// (8)
        name2bit.put("operator_&&", 0x10L);  	// (16)
        name2bit.put("operator_<=", 0x20L);  	// (32)
        name2bit.put("precedence", 0x40L);  	// (64)
        name2bit.put("associativity", 0x80L);  	// (128)
        name2bit.put("operator_!=", 0x100L);  	// (256)
        name2bit.put("operator_>=", 0x200L);  	// (512)
        name2bit.put("operator_binary_-", 0x400L);  	// (1024)
        name2bit.put("operator_||", 0x800L);  	// (2048)
        name2bit.put("operator_&", 0x1000L);  	// (4096)
        name2bit.put("operator_=", 0x2000L);  	// (8192)
        name2bit.put("operator_binary_+", 0x4000L);  	// (16384)
        name2bit.put("operator_/", 0x8000L);  	// (32768)
        name2bit.put("operator_unary_*", 0x10000L);  	// (65536)
        name2bit.put("operator_binary_*", 0x20000L);  	// (131072)
        name2bit.put("operator_<<", 0x40000L);  	// (262144)
        name2bit.put("operator_unary_-", 0x80000L);  	// (524288)
        name2bit.put("operator_|", 0x100000L);  	// (1048576)
        name2bit.put("operator_^", 0x200000L);  	// (2097152)
        name2bit.put("operator_<", 0x400000L);  	// (4194304)
        name2bit.put("operator_>", 0x800000L);  	// (8388608)
        name2bit.put("operator_postfix_++", 0x1000000L);  	// (16777216)
        name2bit.put("operator_binary_&", 0x2000000L);  	// (33554432)
        name2bit.put("operator_%", 0x4000000L);  	// (67108864)
        name2bit.put("operator_postfix_--", 0x8000000L);  	// (134217728)
        name2bit.put("operator_>>", 0x10000000L);  	// (268435456)
        name2bit.put("operator_+=", 0x20000000L);  	// (536870912)
        name2bit.put("operator_|=", 0x40000000L);  	// (1073741824)
        name2bit.put("operator_~", 0x80000000L);  	// (2147483648)
        name2bit.put("operator_&=", 0x100000000L);  	// (4294967296)
        name2bit.put("operator_unary_+", 0x200000000L);  	// (8589934592)
        name2bit.put("operator_-=", 0x400000000L);  	// (17179869184)
        name2bit.put("operator_/=", 0x800000000L);  	// (34359738368)
        name2bit.put("operator_<<=", 0x1000000000L);  	// (68719476736)
        name2bit.put("operator_>>=", 0x2000000000L);  	// (137438953472)
        name2bit.put("operator_(", 0x4000000000L);  	// (274877906944)
        name2bit.put("operator_->", 0x8000000000L);  	// (549755813888)
        name2bit.put("operator_function_call", 0x10000000000L);  	// (1099511627776)
        name2bit.put("operator_.", 0x20000000000L);  	// (2199023255552)
        name2bit.put("operator_subscript", 0x40000000000L);  	// (4398046511104)
        name2bit.put("operator_prefix_++", 0x80000000000L);  	// (8796093022208)
        name2bit.put("operator_prefix_--", 0x100000000000L);  	// (17592186044416)
        return name2bit;
        // (developer tip: see sqlite2mysql)
    }
    private HashMap<String, Long> _fillViolations(HashMap<String, Long> name2bit) {
        // control Flow
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
        // Expression
        name2bit.put("error_base_higher_precedence_right", 0x1L);    // (1)
        name2bit.put("error_base_student_error_early_finish", 0x2L);    // (2)
        name2bit.put("error_base_student_error_in_complex", 0x4L);    // (4)
        name2bit.put("error_base_same_precedence_right_associativity_right", 0x8L);    // (8)
        name2bit.put("error_base_higher_precedence_left", 0x10L);    // (16)
        name2bit.put("error_base_student_error_strict_operands_order", 0x20L);    // (32)
        name2bit.put("error_base_same_precedence_left_associativity_left", 0x40L);    // (64)
        name2bit.put("error_base_student_error_unevaluated_operand", 0x80L);    // (128)    }
        name2bit.put("associativity", 0x100L);  	// (256)
        name2bit.put("error_base_unary_having_associativity_right", 0x200L);  	// (512)
        name2bit.put("precedence", 0x400L);  	// (1024)
        name2bit.put("error_base_binary_having_associativity_left", 0x800L);  	// (2048)
        name2bit.put("error_base_binary_having_associativity_right", 0x1000L);  // (4096)
        name2bit.put("error_base_unary_having_associativity_left", 0x2000L);  	// (8192)
        return name2bit;
    }
    private HashMap<String, Long> _fillLaws(HashMap<String, Long> name2bit) {
        // control Flow (empty)
        // Expression
        name2bit.put("single_token_binary_execution", 0x1L);  	// (1)
        name2bit.put("two_token_binary_execution", 0x2L);  	// (2)
        name2bit.put("single_token_unary_prefix_execution", 0x4L);  	// (4)
        name2bit.put("two_token_unary_execution", 0x8L);  	// (8)
        name2bit.put("single_token_unary_postfix_execution", 0x10L);  	// (16)
        return name2bit;
    }
}
