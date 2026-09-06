package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.Nullable;

/**
 * Разброс сложности вопросов домена в банке.
 * <p>
 * По нему сложность из запроса упражнения нормализуется в шкалу конкретного банка:
 * «средний по сложности» вопрос в пустом и в наполненном банке — разные вопросы.
 *
 * @param min пусто, если вопросов домена в банке нет вовсе
 */
public record ComplexityStatsData(long count, @Nullable Double min, @Nullable Double mean,
                                  @Nullable Double max) {
}
