package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.entities.ResponseEntity;
import org.vstu.compprehension.mappers.Mapping;

/**
 * Ответ студента: пара выбранных вариантов.
 * <p>
 * Признак ошибок считается по взаимодействию целиком, поэтому в самом ответе его нет
 * и приходит он снаружи.
 */
interface ResponseMapper extends Mapping {

    @NotNull ResponseData map(@NotNull ResponseEntity response, boolean interactionHasViolations);
}
