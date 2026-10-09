package org.vstu.compprehension.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Решение тренажёра об ответе студента: какие рассуждения он допускает и что студенту говорит.
 *
 * @param probable идентификаторы рассуждений вердикта, которыми студент мог прийти к ответу: только они предлагаются ему
 *                 и засчитываются, пока он не назвал причину
 */
public record AnswerReaction(@NotNull Set<Integer> probable, @NotNull Reply reply) {

    public AnswerReaction {
        if (probable.isEmpty()) {
            throw new IllegalArgumentException("At least one reasoning is admitted");
        }
        if (reply instanceof Reply.Explain explain && !probable.contains(explain.reasoning())) {
            throw new IllegalArgumentException("Explained reasoning " + explain.reasoning() + " is not admitted");
        }
        if (reply instanceof Reply.Clarify clarify && !probable.containsAll(clarify.options())) {
            throw new IllegalArgumentException("Offered reasonings " + clarify.options() + " are not all admitted");
        }
        probable = Set.copyOf(probable);
    }

    /** Что студент видит в ответ. */
    public sealed interface Reply {

        /** Ответ объясняется этим рассуждением. */
        record Explain(int reasoning) implements Reply {
        }

        /** Студента спрашивают, каким из этих рассуждений он пришёл к ответу; до ответа он видит только вердикт. */
        record Clarify(@NotNull List<Integer> options) implements Reply {

            public Clarify {
                if (options.isEmpty() || Set.copyOf(options).size() != options.size()) {
                    throw new IllegalArgumentException("Options are distinct and not empty: " + options);
                }
                options = List.copyOf(options);
            }
        }

        /** Только вердикт: верно или «не может быть», без причины. */
        record Acknowledge() implements Reply {
        }
    }
}
