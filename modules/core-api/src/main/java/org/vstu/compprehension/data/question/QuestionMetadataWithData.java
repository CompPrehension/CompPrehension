package org.vstu.compprehension.data.question;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;

/**
 * Метаданные вопроса вместе с его сериализованным телом из банка заданий.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionMetadataWithData extends QuestionMetadataData {
    private @NotNull SerializableQuestion data;
}
