package org.vstu.compprehension.data.question;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;

import java.util.Date;

/**
 * Метаданные вопроса из банка заданий.
 * <p>
 * Скалярная копия {@code QuestionMetadataEntity} плюс сам сериализованный вопрос.
 * Вычисляемые маски (пересечения планового, запрошенного и фактического) повторены
 * здесь один в один с сущностью — это единственное место дублирования формул, и оно
 * намеренное: иначе доменам пришлось бы держать сущность ради четырёх методов.
 */
@Data
@Builder
@NoArgsConstructor
@lombok.AllArgsConstructor
public class QuestionMetadataData {
    private Integer id;
    private String name;
    private String domainShortname;
    private String templateId;
    private String qDataGraph;
    private Long tagBits;
    private Long conceptBits;
    private Long lawBits;
    private Long skillBits;
    private Long violationBits;
    private Long traceConceptBits;
    private Double solutionStructuralComplexity;
    private Double integralComplexity;
    private Integer solutionSteps;
    private Integer distinctErrorsCount;
    private Integer version;
    @Builder.Default
    private String structureHash = "";
    @Builder.Default
    private String origin = "";
    @Builder.Default
    private String originLicense = null;
    private Date createdAt;
    private Integer generationRequestId;
    @Builder.Default
    private Long conceptBitsInPlan = 0L;
    @Builder.Default
    private Long conceptBitsInRequest = 0L;
    @Builder.Default
    private Long violationBitsInPlan = 0L;
    @Builder.Default
    private Long violationBitsInRequest = 0L;
    @Builder.Default
    private Long skillBitsInPlan = 0L;

    /** Сериализованный вопрос из банка заданий. */
    private @Nullable SerializableQuestion data;

    /** Общие биты понятий из плана и из вопроса. */
    public Long traceConceptsSatisfiedFromPlan() {
        return (traceConceptBits != 0 ? traceConceptBits : conceptBits) & conceptBitsInPlan;
    }

    /** Понятия из плана, отсутствующие в вопросе. */
    public Long traceConceptsUnsatisfiedFromPlan() {
        return ~(traceConceptBits != 0 ? traceConceptBits : conceptBits) & conceptBitsInPlan;
    }

    /** Общие биты нарушений из плана и из вопроса. */
    public Long violationsSatisfiedFromPlan() {
        return violationBits & violationBitsInPlan;
    }

    /** Нарушения из плана, отсутствующие в вопросе. */
    public Long violationsUnsatisfiedFromPlan() {
        return ~violationBits & violationBitsInPlan;
    }

    /** Общие биты понятий из запроса и из вопроса. */
    public Long traceConceptsSatisfiedFromRequest() {
        return (traceConceptBits != 0 ? traceConceptBits : conceptBits) & conceptBitsInRequest;
    }

    /** Общие биты нарушений из запроса и из вопроса. */
    public Long violationsSatisfiedFromRequest() {
        return violationBits & violationBitsInRequest;
    }
}
