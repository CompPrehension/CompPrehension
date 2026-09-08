package org.vstu.compprehension.mappers;

import org.jetbrains.annotations.NotNull;

/**
 * Перенос значения в уже существующую цель.
 * <p>
 * Форма для изменяемых целей, и в первую очередь для управляемых JPA-сущностей:
 * такую сущность нельзя подменить новым экземпляром, её можно только править —
 * иначе Hibernate потеряет и идентичность, и dirty checking. Если цель создаётся
 * с нуля — это {@link Mapper}, а не этот интерфейс.
 * <p>
 * К реализации те же требования чистоты, что и к {@link Mapper}: только то,
 * что пришло в {@code source}, и вызовы других мапперов.
 *
 * @param <S> тип источника
 * @param <D> тип цели
 * @see Mapper
 * @see Mapping
 */
public interface UpdateMapper<S, D> extends Mapping {

    /** Перенести источник в цель, изменив её на месте. */
    void apply(@NotNull S source, @NotNull D destination);
}
