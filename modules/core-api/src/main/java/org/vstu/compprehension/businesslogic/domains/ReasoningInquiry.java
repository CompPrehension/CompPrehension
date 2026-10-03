package org.vstu.compprehension.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.Explanation;

/**
 * Разговор об ответе, пока рассуждение студента неизвестно.
 *
 * @param prompt    вопрос студенту, каким рассуждением он пришёл к ответу
 * @param statement что сказать об ответе без рассуждения; у верного ответа пусто
 */
public record ReasoningInquiry(@NotNull String prompt, @NotNull Explanation statement) {
}
