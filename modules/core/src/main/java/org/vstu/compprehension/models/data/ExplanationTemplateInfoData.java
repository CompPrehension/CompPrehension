package org.vstu.compprehension.models.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Подстановка в шаблон объяснения нарушения: имя поля и его значение. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplanationTemplateInfoData {
    private Long id;
    private String fieldName;
    private String value;
}
