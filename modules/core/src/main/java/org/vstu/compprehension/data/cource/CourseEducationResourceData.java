package org.vstu.compprehension.data.cource;

/**
 * Пара «курс — его образовательный ресурс».
 * <p>
 * Нужна там, где право, выданное в образовательном ресурсе, надо распространить на все
 * его курсы: для этого от курса требуется ровно эта одна ссылка, и поднимать ради неё
 * курс целиком незачем.
 */
public record CourseEducationResourceData(long courseId, long educationResourceId) {
}
