package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.data.enums.InteractionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Нарушение закона, найденное при разборе ответа студента.
 * <p>
 * Изменяемый контейнер: домены создают такие объекты по ходу разбора (раньше — напрямую
 * {@code ViolationEntity}), а сервис переносит их в сущности при сохранении.
 * <p>
 * {@code BackendFactData} вопреки имени не JPA-сущность, а значение из json-колонки,
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

    /**
     * Тип взаимодействия, в котором нарушение обнаружено; null у только что созданных.
     * <p>
     * Проекция вместо обратной ссылки: из взаимодействия читался только его тип.
     */
    private InteractionType interactionType;
    @Builder.Default
    private List<BackendFactData> violationFacts = new ArrayList<>();
    @Builder.Default
    private List<ExplanationTemplateInfoData> explanationTemplateInfo = new ArrayList<>();
}
