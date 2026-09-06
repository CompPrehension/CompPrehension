package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;

/**
 * Упражнение как строка списка.
 * <p>
 * Отдельный тип, а не наполовину заполненная {@link ExerciseData}: в списке упражнений
 * курса не нужны ни стадии, ни настройки, и по типу должно быть видно, что их здесь
 * нет — а не выясняться при обращении к пустому полю.
 */
public record ExerciseSummaryData(long id, @NotNull String name, boolean isPublic) {
}
