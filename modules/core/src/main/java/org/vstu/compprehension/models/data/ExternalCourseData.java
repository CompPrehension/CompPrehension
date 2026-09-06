package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;

/**
 * Курс, заведённый по курсу внешней системы.
 * <p>
 * Отдельно от {@link CourseSummaryData}: там курс описан для показа пользователю, здесь —
 * для синхронизации с LMS, и внешний идентификатор здесь обязателен. Курс без него в эту
 * выборку не попадает вовсе: синхронизировать нечего.
 */
public record ExternalCourseData(long id, @NotNull String name, @NotNull String externalCourseId) {
}
