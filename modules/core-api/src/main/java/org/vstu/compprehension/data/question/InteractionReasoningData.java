package org.vstu.compprehension.data.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

/** Рассуждение, которым студент мог прийти к ответу, в том виде, в каком оно хранится у взаимодействия. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractionReasoningData implements Serializable {
    /** Идентификатор рассуждения во взаимодействии: по нему на рассуждение ссылаются уточнение и выбор студента. */
    private int id;
    /** Допущения хода мысли, от внешнего к вложенному; пусто — домен ход мысли не установил и объясняет сам ответ. */
    private @NotNull List<Assumption> assumptions = List.of();
    /** Тренажёр допустил рассуждение: только такие предлагаются студенту и засчитываются, пока он не назвал причину. */
    // Ключи JSON — как в хранимых рассуждениях; без них Jackson назвал бы свойства по геттерам: probable, correct.
    @JsonProperty("isProbable")
    private boolean probable;
    @JsonProperty("isCorrect")
    private boolean correct;
    private @Nullable String reason;
    private @NotNull List<ViolationData> violations = List.of();
    private @NotNull List<String> appliedLaws = List.of();
}
