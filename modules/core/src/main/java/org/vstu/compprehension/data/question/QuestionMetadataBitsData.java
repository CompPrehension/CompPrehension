package org.vstu.compprehension.data.question;

/**
 * Битовые маски метаданных вопроса, по которым стратегии балансируют цели.
 * <p>
 * Значения вычисляет сама {@code QuestionMetadataEntity} — здесь только снятый результат,
 * чтобы формулы остались в одном месте.
 */
public record QuestionMetadataBitsData(
        int id,
        long conceptsSatisfiedFromPlan,
        long conceptsUnsatisfiedFromPlan,
        long conceptsSatisfiedFromRequest,
        long conceptBitsInRequest,
        long violationsSatisfiedFromPlan,
        long violationsUnsatisfiedFromPlan,
        long violationsSatisfiedFromRequest,
        long violationBitsInRequest,
        Long skillBits) {
}
