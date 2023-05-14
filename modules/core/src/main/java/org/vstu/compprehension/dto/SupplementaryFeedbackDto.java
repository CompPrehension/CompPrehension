package org.vstu.compprehension.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.dto.feedback.FeedbackDto;

@AllArgsConstructor
@Getter @Setter
public class SupplementaryFeedbackDto {
    private @NotNull FeedbackDto.Message message;
    private @NotNull Action action;

    public enum Action {
        @JsonProperty("CONTINUE_AUTO")
        ContinueAuto,
        @JsonProperty("CONTINUE_MANUAL")
        ContinueManual,
        @JsonProperty("FINISH")
        Finish,
    }
}
