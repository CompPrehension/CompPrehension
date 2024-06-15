package org.vstu.compprehension.models.businesslogic;

import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.common.MathHelper;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;

import java.util.List;

@Builder
@Getter
public class QuestionBankSearchRequest {
    private long deniedConceptsBitmask;
    private long targetConceptsBitmask;
    private long targetLawsBitmask;
    private long deniedLawsBitmask;
    private long targetTagsBitmask;

    private @Nullable List<String> deniedQuestionNames;
    private @Nullable List<String> deniedQuestionTemplateIds;
    private @Nullable List<Integer> deniedQuestionMetaIds;

    private String domainShortname;

    /**
     * Сложность задания [0..1]
     */
    private float complexity;

    /** минимум шагов в решении */
    private int stepsMin;

    /** максимум шагов в решении */
    private int stepsMax;

    public static QuestionBankSearchRequest fromQuestionRequest(QuestionRequest qr, double bankMinComplexity, double bankMaxComplexity) {
        var normalizedComplexity = MathHelper.linearInterpolateToNewRange(
                qr.getComplexity(),
                0,
                1,
                (float)bankMinComplexity,
                (float)bankMaxComplexity);

        return QuestionBankSearchRequest.builder()
                .deniedConceptsBitmask(Concept.combineToBitmask(qr.getDeniedConcepts()))
                .targetConceptsBitmask(Concept.combineToBitmask(qr.getTargetConcepts()))
                .targetLawsBitmask(Law.combineToBitmask(qr.getTargetLaws()))
                .deniedLawsBitmask(Law.combineToBitmask(qr.getDeniedLaws()))
                .targetTagsBitmask(Tag.combineToBitmask(qr.getTargetTags()))
                .deniedQuestionNames(qr.getDeniedQuestionNames())
                .deniedQuestionTemplateIds(qr.getDeniedQuestionTemplateIds())
                .deniedQuestionMetaIds(qr.getDeniedQuestionMetaIds())
                .domainShortname(qr.getDomainShortname())
                .complexity(normalizedComplexity)
                .stepsMin(qr.getStepsMin())
                .stepsMax(qr.getStepsMax())
                .build();
    }

    public static QuestionBankSearchRequest fromQuestionRequestLog(QuestionRequestLogEntity qr, double bankMinComplexity, double bankMaxComplexity) {
        var normalizedComplexity = MathHelper.linearInterpolateToNewRange(
                qr.getComplexity(),
                0,
                1,
                (float)bankMinComplexity,
                (float)bankMaxComplexity);

        return QuestionBankSearchRequest.builder()
                .deniedConceptsBitmask(qr.getConceptsDeniedBitmask())
                .targetConceptsBitmask(qr.getConceptsTargetedBitmask())
                .targetLawsBitmask(qr.getLawsTargetedBitmask())
                .deniedLawsBitmask(qr.getLawsDeniedBitmask())
                .targetTagsBitmask(qr.getTargetTagsBitmask())
                .deniedQuestionNames(qr.getDeniedQuestionNames())
                .deniedQuestionTemplateIds(qr.getDeniedQuestionTemplateIds())
                .deniedQuestionMetaIds(qr.getDeniedQuestionMetaIds())
                .domainShortname(qr.getDomainShortname())
                .complexity(normalizedComplexity)
                .stepsMin(qr.getStepsMin())
                .stepsMax(qr.getStepsMax())
                .build();
    }
}
