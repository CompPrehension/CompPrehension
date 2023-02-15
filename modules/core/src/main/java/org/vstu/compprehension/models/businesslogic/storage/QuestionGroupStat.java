package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Data;
import org.vstu.compprehension.models.businesslogic.storage.stats.BitmaskStat;
import org.vstu.compprehension.models.businesslogic.storage.stats.NumericStat;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.Collection;
import java.util.stream.Collectors;

@Data
public class QuestionGroupStat {

    BitmaskStat tagStat;
    BitmaskStat conceptStat;
    BitmaskStat traceConceptStat;
//    BitmaskStat lawStat;
    BitmaskStat violationStat;
    NumericStat complexityStat;  // integralComplexity
    NumericStat solutionStepsStat;  // solutionSteps

    public QuestionGroupStat() {
    }

    @Deprecated
    public QuestionGroupStat(Collection<QuestionMetadataEntity> questionSet) {
        super();

        // some fields that are currently unused are out-commented below.
//        tagStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataEntity::getTagBits).collect(Collectors.toList()));
//        conceptStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataEntity::getConceptBits).collect(Collectors.toList()));
//        traceConceptStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataEntity::getTraceConceptBits).collect(Collectors.toList()));
//        lawStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataBaseEntity::getLawBits).collect(Collectors.toList()));
//        violationStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataEntity::getViolationBits).collect(Collectors.toList()));

        complexityStat = new NumericStat(questionSet.stream().map(QuestionMetadataEntity::getIntegralComplexity).collect(Collectors.toList()), false);
//        solutionStepsStat = new NumericStat(questionSet.stream().map(QuestionMetadataEntity::getSolutionSteps).map(Double::valueOf).collect(Collectors.toList()), false);
    }

}
