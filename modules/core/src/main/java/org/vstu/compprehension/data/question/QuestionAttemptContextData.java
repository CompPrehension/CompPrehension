package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.data.enums.Language;

import java.util.List;

/**
 * Что известно о попытке и упражнении по одному заданному в них вопросу.
 * <p>
 * Один тип на все вопросы сервиса — язык автора, этапы упражнения, идентификатор
 * попытки, стратегия, режим вспомогательных вопросов, — потому что все они отвечаются
 * одной строкой. Раньше каждый из них поднимал попытку отдельно, и вместе с ней все её
 * вопросы: запрос {@code findByQuestionId} тянул {@code left join fetch a.questions}
 * ради одного поля.
 *
 * @param stages пустой список, если у упражнения нет этапов
 * @param strategyId стратегия упражнения: по ней считаются оценка и решение о том,
 *                   продолжать ли попытку
 */
public record QuestionAttemptContextData(
        long attemptId,
        @NotNull Language userLanguage,
        @NotNull String strategyId,
        @NotNull List<ExerciseStageData> stages,
        boolean preferDecisionTreeSupplementary) {
}
