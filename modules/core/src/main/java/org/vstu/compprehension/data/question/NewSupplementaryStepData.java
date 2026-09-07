package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * Новый шаг цепочки вспомогательных вопросов — то, что домен просит записать.
 * <p>
 * Отдельный тип от {@link SupplementaryStepData}: у ещё не сохранённого шага нет
 * идентификатора, а связь со сгенерированным вопросом проставляет сервис — до записи
 * вопроса её попросту не существует. Раньше домен для этого собирал
 * {@code SupplementaryStepEntity} и отдавал сущность наружу.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewSupplementaryStepData {
    private long mainQuestionInteractionId;
    private SupplementarySituationData situationInfo;
    private @Nullable Integer nextStateId;
}
