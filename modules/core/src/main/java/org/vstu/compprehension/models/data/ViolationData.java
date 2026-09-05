package org.vstu.compprehension.models.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Нарушение закона, найденное при разборе ответа студента.
 * <p>
 * Изменяемый контейнер: домены создают такие объекты по ходу разбора (раньше — напрямую
 * {@code ViolationEntity}), а сервис переносит их в сущности при сохранении.
 * <p>
 * {@code BackendFactEntity} вопреки имени не JPA-сущность, а значение из json-колонки,
 * поэтому используется как есть.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationData {
    private Long id;
    private String lawName;
    private String detailedLawName;
    @Builder.Default
    private List<BackendFactEntity> violationFacts = new ArrayList<>();
    @Builder.Default
    private List<ExplanationTemplateInfoData> explanationTemplateInfo = new ArrayList<>();
}
