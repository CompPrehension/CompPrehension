package org.vstu.compprehension.businesslogic.strategies.settings;

/** Какие рассуждения, которыми студент мог прийти к ответу, тренажёр допускает. */
public enum ReasoningSelection {
    @Setting(ru = "Самые вероятные — с наименьшим числом ошибок", en = "Most probable — with the fewest errors")
    FEWEST_ERRORS,
    @Setting(ru = "Все", en = "All")
    ALL
}
