package org.vstu.compprehension.frontend.dto;

/** Ссылка на упражнение курса — столько, сколько нужно, чтобы показать его в списке. */
public record ExerciseRefDto(long exerciseId, String name) {
}
