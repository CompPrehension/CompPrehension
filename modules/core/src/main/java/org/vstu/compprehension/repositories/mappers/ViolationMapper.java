package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.ViolationEntity;
import org.vstu.compprehension.mappers.Mapping;

interface ViolationMapper extends Mapping {

    @NotNull ViolationData map(@NotNull ViolationEntity violation, @NotNull InteractionEntity owner);
}
