package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;

/**
 * Куда и за кого отправлять оценку за попытку.
 * <p>
 * Ровно то, что читают стратегии выставления оценки, — и ничего сверх того. Раньше им
 * передавалась {@code ExerciseAttemptEntity}, и каждая ходила по её ленивым связям:
 * пользователь, курс, образовательный ресурс курса. Отправка асинхронная, так что
 * сессия к тому моменту могла быть уже чужой.
 *
 * @param externalUserId  идентификатор пользователя во внешней системе; null, если он
 *                        ни разу не входил через LTI
 * @param ltiLineitemUrl  колонка журнала LTI AGS; null, если попытка запущена не из LMS
 * @param course          null, если попытка идёт вне курса
 */
public record GradePassbackTargetData(
        long attemptId,
        long exerciseId,
        long userId,
        @Nullable String externalUserId,
        @Nullable String ltiLineitemUrl,
        @Nullable CourseTarget course) {

    /**
     * @param externalCourseId идентификатор курса во внешней системе; null, если курс
     *                         заведён вручную и с LMS не связан
     */
    public record CourseTarget(
            long courseId,
            @Nullable String externalCourseId,
            @NotNull EducationResourceTarget educationResource) {
    }

    /** Образовательный ресурс, из которого пришёл курс. */
    public record EducationResourceTarget(
            long id,
            @NotNull EducationResourceType type,
            @NotNull String url) {
    }
}
