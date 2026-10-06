package org.vstu.compprehension.businesslogic.strategies.settings;

/** Когда спрашивать о рассуждении, которым студент пришёл к неверному ответу. */
public enum WrongAnswerClarification {
    @Setting(ru = "Если причин несколько", en = "If there are several reasons")
    WHEN_AMBIGUOUS,
    @Setting(ru = "Всегда, если есть причина", en = "Always if there is a reason")
    ALWAYS
}
