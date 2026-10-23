package org.vstu.compprehension.data.question;

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
