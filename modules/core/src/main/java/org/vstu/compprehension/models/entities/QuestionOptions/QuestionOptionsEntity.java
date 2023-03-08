package org.vstu.compprehension.models.entities.QuestionOptions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.io.Serializable;

/**
  Base class for question options
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "_type")
@JsonSubTypes({
    // defines mapping to subtypes
    @JsonSubTypes.Type(value = OrderQuestionOptionsEntity.class, name = "OrderQuestionOptionsEntity"),
    @JsonSubTypes.Type(value = MatchingQuestionOptionsEntity.class, name = "MatchingQuestionOptionsEntity"),
    @JsonSubTypes.Type(value = MultiChoiceOptionsEntity.class, name = "MultiChoiceOptionsEntity"),
    @JsonSubTypes.Type(value = SingleChoiceOptionsEntity.class, name = "SingleChoiceOptionsEntity"),
})
@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder @Jacksonized
public class QuestionOptionsEntity implements Serializable {
    /// Question text contains answers
    @Builder.Default
    protected boolean requireContext = false;
    @Builder.Default
    private boolean showSupplementaryQuestions = true;
    @Builder.Default
    private int templateId = -1;
    @Builder.Default
    private int questionMetaId = -1;
    @Builder.Default
    private QuestionMetadataEntity metadata = null;
}
