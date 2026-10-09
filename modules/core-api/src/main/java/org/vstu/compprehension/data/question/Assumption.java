package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/** Допущение в рассуждении студента: шаг хода мысли и верен ли он. */
public record Assumption(@NotNull String hypothesis, boolean isCorrect) implements Serializable {
}
