package org.vstu.compprehension.models.entities.QuestionOptions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
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
    @JsonSubTypes.Type(value = OrderQuestionOptionsEntity.class, name = "OrderQuestionOptionsEntity"),
    @JsonSubTypes.Type(value = MatchingQuestionOptionsEntity.class, name = "MatchingQuestionOptionsEntity"),
})
@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder @Jacksonized
public class QuestionOptionsEntity implements Serializable {
    /// Question text contains answers
    private boolean requireContext;
}
