package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;

/**
 * Описание предметной области, с которым работает {@code Domain}.
 * <p>
 * Заменяет {@code DomainEntity} в контракте: домену нужны только эти четыре поля,
 * а JPA-сущность тянула за собой зависимость от способа хранения. Отображение
 * сущности в эту запись делает фабрика доменов при старте.
 *
 * @param name    идентификатор области, он же первичный ключ в БД
 * @param options вопреки имени класса, {@code DomainOptionsEntity} — не JPA-сущность,
 *                а значение из json-колонки {@code options_json}
 */
public record DomainData(
        @NotNull String name,
        @NotNull String shortName,
        @NotNull String version,
        DomainOptionsEntity options) {
}
