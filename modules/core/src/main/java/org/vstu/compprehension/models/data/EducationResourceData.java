package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceTrustStatus;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;

/**
 * Внешняя образовательная система (LMS), из которой к нам приходят по LTI.
 * <p>
 * Заменяет {@code EducationResourceEntity} в контракте сервиса: наружу от ресурса нужны
 * идентификатор — чтобы привязать к нему учётную запись, курс и роли — и статус доверия,
 * по которому решается, пускать ли LTI-привязку вообще. Ленивых связей у сущности нет,
 * но её появление в сигнатуре означало, что вызывающий обязан находиться в транзакции.
 */
public record EducationResourceData(
        long id,
        @NotNull String url,
        @NotNull EducationResourceType type,
        @NotNull EducationResourceTrustStatus trustStatus) {
}
