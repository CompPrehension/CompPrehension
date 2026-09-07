package org.vstu.compprehension.data.cource;

import org.jetbrains.annotations.NotNull;

/**
 * Упражнение как элемент курса: столько, сколько нужно, чтобы сослаться на него в меню.
 * <p>
 * Заменяет обход {@code ExerciseCourseLinkEntity.getExercise()} за пределами транзакции:
 * связь ленивая, и вызывающий получал прокси, который вне сессии не инициализируется.
 */
public record CourseExerciseData(long exerciseId, @NotNull String name) {
}
