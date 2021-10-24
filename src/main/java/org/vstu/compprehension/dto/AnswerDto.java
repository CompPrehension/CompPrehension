package org.vstu.compprehension.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder @Jacksonized
@AllArgsConstructor
public class AnswerDto {
    public AnswerDto(Long leftResponseId, Long rightResponseId, boolean isCreatedByUser) {
        this.answer = new Long[] { leftResponseId, rightResponseId };
        this.isCreatedByUser = isCreatedByUser;
    }

    private Long[] answer;
    @JsonProperty("createdByUser")
    private boolean isCreatedByUser;
}
