package org.vstu.compprehension.data.exercise;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.RoleInExercise;
import org.vstu.compprehension.frontend.dto.ExerciseConceptDto;
import org.vstu.compprehension.frontend.dto.ExerciseLawDto;
import org.vstu.compprehension.frontend.dto.ExerciseSkillDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Value
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExerciseStageData implements Serializable {
    @NotNull String strategyId;

    // Тип настроек знает только стратегия этапа: здесь они хранятся как есть.
    @Builder.Default
    @NotNull Map<String, Object> strategySettings = Map.of();

    /** Сколько вопросов задать на этапе; стратегия может закончить этап раньше. */
    @Builder.Default
    int numberOfQuestions = 5;

    @Builder.Default
    float complexity = 0.5f;

    @Builder.Default
    @NotNull List<ExerciseLawDto> laws = new ArrayList<>(0);

    @Builder.Default
    @NotNull List<ExerciseConceptDto> concepts = new ArrayList<>(0);

    @Builder.Default
    @NotNull List<ExerciseSkillDto> skills = new ArrayList<>(0);
}
