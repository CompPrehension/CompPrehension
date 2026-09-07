package org.vstu.compprehension.data.cource;

import org.jetbrains.annotations.NotNull;

/**
 * Курс в объёме, нужном для списков курсов.
 * <p>
 * Образовательный ресурс приходит вместе с курсом, а не ссылкой на него: у курса он
 * обязателен схемой, а показывается всегда рядом с именем курса. Отдельный тип, а не
 * урезанный «курс целиком», — чтобы по типу было видно, что здесь нет ни внешнего
 * идентификатора курса, ни его упражнений.
 *
 * @param educationResourceUrl у образовательного ресурса нет имени — подписью служит адрес
 */
public record CourseSummaryData(
        long id,
        @NotNull String name,
        long educationResourceId,
        @NotNull String educationResourceUrl) {
}
