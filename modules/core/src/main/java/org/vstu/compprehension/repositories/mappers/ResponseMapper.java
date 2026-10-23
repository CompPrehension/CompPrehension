package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.entities.ResponseEntity;
import org.vstu.compprehension.mappers.Mapping;

interface ResponseMapper extends Mapping {

    @NotNull ResponseData map(@NotNull ResponseEntity response, boolean interactionHasViolations);
}
