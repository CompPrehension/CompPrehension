package org.vstu.compprehension.enums;

/**
 * Результат проверки ответа студента по одной ветви рассуждений вспомогательных вопросов.
 */
public enum SupplementaryBranchResult {
    /**
     * Ответ корректен
     */
    CORRECT,

    /**
     * Допущена ошибка
     */
    ERROR,

    /**
     * Невозможно определить корректность ответа
     */
    NULL,
}
