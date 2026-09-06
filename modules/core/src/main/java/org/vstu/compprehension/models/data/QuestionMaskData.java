package org.vstu.compprehension.models.data;

/**
 * Чем «занят» вопрос: какие понятия, законы, нарушения и умения в нём встречаются.
 * <p>
 * Нужна там, где о вопросе не требуется знать ничего, кроме этого: банк смотрит на
 * недавние вопросы попытки, чтобы не выдать похожий. Отдельный тип, а не урезанные
 * {@link QuestionMetadataData}, — потому что тела вопроса здесь нет и не будет.
 */
public record QuestionMaskData(long conceptBits, long lawBits, long violationBits, long skillBits) {
}
