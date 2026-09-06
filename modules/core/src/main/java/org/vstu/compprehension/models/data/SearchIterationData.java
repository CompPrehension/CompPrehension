package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;

/**
 * Одна попытка поиска в банке: с каким качеством искали, сколько просили и сколько нашли.
 * <p>
 * Поиск идёт по убыванию требовательности, и в журнал уезжает вся цепочка — по ней
 * видно, на каком шаге банк перестал справляться.
 */
public record SearchIterationData(@NotNull SearchQuality quality, @NotNull Integer limit,
                                  @NotNull Integer found) {
}
