package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AnswerObjectData {
    private Long id;
    private Integer answerId;
    private String hyperText;
    private String domainInfo;
    private boolean isRightCol;
    private String concept;
}
