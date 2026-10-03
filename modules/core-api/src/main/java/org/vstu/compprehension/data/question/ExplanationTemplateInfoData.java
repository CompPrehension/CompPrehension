package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplanationTemplateInfoData implements Serializable {
    private Long id;
    private String fieldName;
    private String value;
}
