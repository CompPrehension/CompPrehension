package org.vstu.compprehension.data.questionoptions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

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
    @JsonSubTypes.Type(value = OrderQuestionOptionsData.class, name = "OrderQuestionOptionsEntity"),
    @JsonSubTypes.Type(value = MatchingQuestionOptionsData.class, name = "MatchingQuestionOptionsEntity"),
    @JsonSubTypes.Type(value = MultiChoiceOptionsData.class, name = "MultiChoiceOptionsEntity"),
    @JsonSubTypes.Type(value = SingleChoiceOptionsData.class, name = "SingleChoiceOptionsEntity"),
})
@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder @Jacksonized
public class QuestionOptionsData implements Serializable {
    /// Question text contains answers
    @Builder.Default
    protected boolean requireContext = false;
    @Builder.Default
    private boolean showSupplementaryQuestions = true;
}
