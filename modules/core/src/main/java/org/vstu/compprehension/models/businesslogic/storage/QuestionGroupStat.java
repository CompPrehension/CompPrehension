package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Data;
import org.vstu.compprehension.models.businesslogic.storage.stats.BitmaskStat;
import org.vstu.compprehension.models.businesslogic.storage.stats.NumericStat;
import org.vstu.compprehension.models.entities.QuestionMetadataBaseEntity;

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

    public QuestionGroupStat(Collection<QuestionMetadataBaseEntity> questionSet) {
        super();

        tagStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataBaseEntity::getTagBits).collect(Collectors.toList()));
        conceptStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataBaseEntity::getConceptBits).collect(Collectors.toList()));
//        lawStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataBaseEntity::getLawBits).collect(Collectors.toList()));
        violationStat = new BitmaskStat(questionSet.stream().map(QuestionMetadataBaseEntity::getViolationBits).collect(Collectors.toList()));

        complexityStat = new NumericStat(questionSet.stream().map(QuestionMetadataBaseEntity::getIntegralComplexity).collect(Collectors.toList()), false);
        solutionStepsStat = new NumericStat(questionSet.stream().map(QuestionMetadataBaseEntity::getSolutionSteps).map(Double::valueOf).collect(Collectors.toList()), false);
    }

}
