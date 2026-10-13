package org.vstu.compprehension.businesslogic.reaction;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.domains.Judgement;
import org.vstu.compprehension.businesslogic.domains.Reasoning;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.enums.InteractionType;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Как тренажёр по настройкам упражнения реагирует на ответ студента, который объясняют рассуждения. */
public final class AnswerReactionPolicy {

    /** Реакция на ответ, который ещё не записан в попытку. */
    public @NotNull AnswerReaction reactToAnswer(@NotNull ExerciseAttemptWithQuestionsData attempt,
                                                 @NotNull Judgement.Reasoned judgement) {
        var settings = AnswerReactionSettings.TYPE.read(attempt.exercise().answerReaction());
        var probable = switch (settings.reasoningSelection()) {
            case FEWEST_ERRORS -> selectFewestErrorReasonings(judgement.reasonings());
            case ALL -> judgement.collectReasoningIds();
        };
        // Предложить на выбор можно только рассуждение с причиной.
        var offered = judgement.reasonings().stream()
                .filter(reasoning -> probable.contains(reasoning.id()) && reasoning.reason() != null)
                .toList();
        var reply = judgement.isAnswerCorrect()
                ? replyToCorrectAnswer(offered, attempt, settings.correctAnswerClarification())
                : replyToWrongAnswer(probable, offered, settings.wrongAnswerClarification());
        return new AnswerReaction(probable, reply);
    }

    // Верный ответ объяснять не нужно; спросить о нём есть смысл, только если к нему ведут и заблуждения.
    private static @NotNull AnswerReaction.Reply replyToCorrectAnswer(@NotNull List<Reasoning> offered,
                                                                    @NotNull ExerciseAttemptWithQuestionsData attempt,
                                                                    @NotNull CorrectAnswerClarification clarification) {
        boolean isMisreasoningOffered = offered.stream().anyMatch(Reasoning::isCorrect)
                && offered.stream().anyMatch(reasoning -> !reasoning.isCorrect());
        return isMisreasoningOffered && shouldClarifyCorrectAnswer(attempt, clarification)
                ? new AnswerReaction.Reply.Clarify(offered.stream().map(Reasoning::id).toList())
                : new AnswerReaction.Reply.Acknowledge();
    }

    // Пока рассуждение неверного ответа не известно, студент видит только вердикт. Если спрашивать велено всегда,
    // уточняется и единственная причина: студент может её отвергнуть.
    private static @NotNull AnswerReaction.Reply replyToWrongAnswer(@NotNull Set<Integer> probable,
                                                                  @NotNull List<Reasoning> offered,
                                                                  @NotNull WrongAnswerClarification clarification) {
        boolean isClarified = switch (clarification) {
            case WHEN_AMBIGUOUS -> offered.size() > 1;
            case ALWAYS -> !offered.isEmpty();
        };
        if (isClarified) {
            return new AnswerReaction.Reply.Clarify(offered.stream().map(Reasoning::id).toList());
        }
        return probable.size() == 1
                ? new AnswerReaction.Reply.Explain(probable.iterator().next())
                : new AnswerReaction.Reply.Acknowledge();
    }

    // Самые вероятные рассуждения — верные и ошибочные с наименьшим числом ошибок. Сочетания ошибок, которые привели
    // к верному ответу, остаются, если других ошибочных рассуждений нет.
    private static @NotNull Set<Integer> selectFewestErrorReasonings(@NotNull List<Reasoning> reasonings) {
        long fewestErrors = reasonings.stream()
                .filter(reasoning -> !reasoning.isCorrect())
                .mapToLong(Reasoning::countErrors)
                .min()
                .orElse(0);
        return reasonings.stream()
                .filter(reasoning -> reasoning.isCorrect() || reasoning.countErrors() == fewestErrors)
                .map(Reasoning::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean shouldClarifyCorrectAnswer(@NotNull ExerciseAttemptWithQuestionsData attempt,
                                                      @NotNull CorrectAnswerClarification clarification) {
        return switch (clarification.mode()) {
            case NEVER -> false;
            case ALWAYS -> true;
            case UNTIL_STREAK -> countConfirmedCorrectStreak(attempt) < clarification.streakLength();
        };
    }

    // Серия — последние подряд верные ответы студента, рассуждение которых, если о нём спрашивали, подтвердилось.
    // Неверный ответ, подсказка и неподтверждённое рассуждение её обрывают.
    private static int countConfirmedCorrectStreak(@NotNull ExerciseAttemptWithQuestionsData attempt) {
        var latestFirst = attempt.questions().stream()
                .flatMap(question -> question.interactions().stream())
                .sorted(Comparator.comparingLong(AttemptQuestionInteractionData::interactionId).reversed())
                .toList();
        int streak = 0;
        for (var interaction : latestFirst) {
            if (interaction.type() != InteractionType.SEND_RESPONSE || !interaction.isCorrect()
                    || Boolean.FALSE.equals(interaction.isReasoningConfirmed())) {
                break;
            }
            streak++;
        }
        return streak;
    }
}
