package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.Nullable;

/**
 * Кому принадлежит попытка и в каком курсе она проходится.
 * <p>
 * Оба поля допускают null: попытка может идти вне курса, а ссылка на пользователя —
 * оказаться пустой в старых данных. Проверка на это была в коде и до переноса.
 */
public record AttemptOwnerData(@Nullable Long userId, @Nullable Long courseId) {
}
