package org.vstu.compprehension.frontend.dto.feedback;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data @SuperBuilder
public class OrderQuestionFeedbackDto extends FeedbackDto {
    private String[] trace;
}
