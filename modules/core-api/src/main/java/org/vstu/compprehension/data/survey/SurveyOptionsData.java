package org.vstu.compprehension.data.survey;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

/**
 * Настройки показа опроса.
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
