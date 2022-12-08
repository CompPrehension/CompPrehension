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
//    BitmaskStat lawStat;
    BitmaskStat violationStat;
    NumericStat complexityStat;  // integralComplexity
    NumericStat solutionStepsStat;  // solutionSteps

    public QuestionGroupStat(Collection<QuestionMetadataEntity> questionSet) {
        super();

        tagStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataEntity::getTagBits).map(Long::valueOf).collect(Collectors.toList()));
        conceptStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataEntity::getConceptBits).map(Long::valueOf).collect(Collectors.toList()));
//        lawStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataBaseEntity::getLawBits).map(Long::valueOf).collect(Collectors.toList()));
        violationStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataEntity::getViolationBits).map(Long::valueOf).collect(Collectors.toList()));

        complexityStat = new NumericStat(questionSet.stream().map(QuestionMetadataEntity::getIntegralComplexity).collect(Collectors.toList()), false);
        solutionStepsStat = new NumericStat(questionSet.stream().map(QuestionMetadataEntity::getSolutionSteps).map(Double::valueOf).collect(Collectors.toList()), false);
    }

}
