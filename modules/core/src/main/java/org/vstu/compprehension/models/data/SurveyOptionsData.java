package org.vstu.compprehension.models.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

/**
 * Настройки показа опроса.
 * <p>
 * Значение json-колонки {@code surveys.options_json}. Раньше жило рядом с сущностями под
 * именем {@code SurveyOptionsEntity} и оттуда же уезжало во фронтовый контракт — то есть
 * web зависел от пакета сущностей, хотя сущностью этот класс никогда не был.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SurveyOptionsData {
    @NotNull Integer size;
}
