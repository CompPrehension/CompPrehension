package org.vstu.compprehension.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data @SuperBuilder
public class OrderQuestionFeedbackDto extends FeedbackDto {
    private String[] trace;
}
