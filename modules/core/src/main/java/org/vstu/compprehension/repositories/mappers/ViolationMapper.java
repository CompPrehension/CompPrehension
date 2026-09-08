package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.ViolationEntity;
import org.vstu.compprehension.mappers.Mapping;

/**
 * Нарушение: шаблоны объяснений обязаны быть подгружены.
 * <p>
 * Вид взаимодействия хранится не у нарушения, а у его владельца. Обратная ссылка
 * {@code ViolationEntity.interaction} ленивая, поэтому владелец приходит вторым
 * аргументом — так маппинг не зависит от того, открыта ли ещё сессия.
 */
interface ViolationMapper extends Mapping {

    @NotNull ViolationData map(@NotNull ViolationEntity violation, @NotNull InteractionEntity owner);
}
